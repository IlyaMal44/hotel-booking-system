package com.example.booking.client;

import com.example.booking.dto.ConfirmCommand;
import com.example.booking.dto.ConfirmView;
import com.example.booking.dto.ReleaseCommand;
import com.example.booking.dto.RoomView;
import com.example.booking.exception.BadRequestException;
import com.example.booking.exception.HotelTransientException;
import com.example.booking.exception.NotFoundException;
import com.example.booking.exception.RoomConflictException;
import com.example.common.Traces;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

@Slf4j
@Component
@Profile("!test")
public class HotelClient implements HotelPort {

    private static final ParameterizedTypeReference<List<RoomView>> ROOM_LIST = new ParameterizedTypeReference<>() {
    };

    private final RestClient restClient;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;
    private final TimeLimiter timeLimiter;
    private final ExecutorService executor;

    public HotelClient(@Qualifier("hotelRestClient") RestClient restClient,
                       RetryRegistry retryRegistry,
                       CircuitBreakerRegistry circuitBreakerRegistry,
                       TimeLimiterRegistry timeLimiterRegistry,
                       @Qualifier("hotelCallExecutor") ExecutorService executor) {
        this.restClient = restClient;
        this.retry = retryRegistry.retry("hotelService");
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("hotelService");
        this.timeLimiter = timeLimiterRegistry.timeLimiter("hotelService");
        this.executor = executor;
    }

    @Override
    /** Запрашивает у отеля список номеров для автоподбора */
    public List<RoomView> recommend(Long hotelId, LocalDate startDate, LocalDate endDate) {
        HttpHeaders headers = captureHeaders();
        return invoke(() -> restClient.get()
                .uri(builder -> builder.path("/api/rooms/recommend")
                        .queryParamIfPresent("hotelId", Optional.ofNullable(hotelId))
                        .queryParam("startDate", startDate)
                        .queryParam("endDate", endDate)
                        .build())
                .headers(http -> http.addAll(headers))
                .retrieve()
                .onStatus(HttpStatusCode::isError, HotelClient::mapStatus)
                .body(ROOM_LIST));
    }

    @Override
    /** Просит отель заблокировать номер на даты брони */
    public ConfirmView confirm(ConfirmCommand command) {
        HttpHeaders headers = captureHeaders();
        headers.set(Traces.BOOKING_HEADER, String.valueOf(command.bookingId()));
        return invoke(() -> restClient.post()
                .uri("/api/rooms/{id}/confirm-availability", command.roomId())
                .headers(http -> http.addAll(headers))
                .body(new ConfirmBody(command.requestId(), command.bookingId(), command.startDate(), command.endDate()))
                .retrieve()
                .onStatus(HttpStatusCode::isError, HotelClient::mapStatus)
                .body(ConfirmView.class));
    }

    @Override
    /** Просит отель снять блокировку номера */
    public void release(ReleaseCommand command) {
        HttpHeaders headers = captureHeaders();
        headers.set(Traces.BOOKING_HEADER, String.valueOf(command.bookingId()));
        invoke(() -> restClient.post()
                .uri("/api/rooms/{id}/release", command.roomId())
                .headers(http -> http.addAll(headers))
                .body(new ReleaseBody(command.requestId(), command.bookingId()))
                .retrieve()
                .onStatus(HttpStatusCode::isError, HotelClient::mapStatus)
                .toBodilessEntity());
    }

    /** Гоняет вызов через circuit breaker, затем через повторы */
    private <T> T invoke(Supplier<T> action) {
        Supplier<T> timed = () -> callWithTimeout(action);
        Supplier<T> decorated = CircuitBreaker.decorateSupplier(circuitBreaker, Retry.decorateSupplier(retry, timed));
        try {
            return decorated.get();
        } catch (CallNotPermittedException exception) {
            log.warn("hotel circuit breaker is open");
            throw new HotelTransientException("Hotel circuit breaker is open", exception);
        }
    }

    /** Ограничивает один вызов по времени и переводит обрыв сети в ошибку для повтора */
    private <T> T callWithTimeout(Supplier<T> action) {
        try {
            return timeLimiter.executeFutureSupplier(() -> CompletableFuture.supplyAsync(() -> {
                try {
                    return action.get();
                } catch (ResourceAccessException exception) {
                    throw new HotelTransientException("Hotel call timed out", exception);
                }
            }, executor));
        } catch (RoomConflictException | NotFoundException | BadRequestException | HotelTransientException exception) {
            throw exception;
        } catch (Exception exception) {
            Throwable cause = unwrap(exception);
            if (cause instanceof RoomConflictException conflict) {
                throw conflict;
            }
            if (cause instanceof NotFoundException notFound) {
                throw notFound;
            }
            if (cause instanceof BadRequestException badRequest) {
                throw badRequest;
            }
            if (cause instanceof HotelTransientException transientFailure) {
                throw transientFailure;
            }
            throw new HotelTransientException("Hotel call failed", cause);
        }
    }

    /** 409 и 404 не повторяем, 5xx отдаем на retry */
    private static void mapStatus(org.springframework.http.HttpRequest request, ClientHttpResponse response) throws IOException {
        String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        int status = response.getStatusCode().value();
        if (status == 409) {
            throw new RoomConflictException(body.isBlank() ? "Room is not available" : body);
        }
        if (status == 404) {
            throw new NotFoundException(body.isBlank() ? "Room not found" : body);
        }
        if (status >= 500) {
            throw new HotelTransientException("Hotel service error " + status);
        }
        throw new BadRequestException(body.isBlank() ? "Hotel rejected the request" : body);
    }

    private HttpHeaders captureHeaders() {
        HttpHeaders headers = new HttpHeaders();
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            copy(request, headers, HttpHeaders.AUTHORIZATION);
            copy(request, headers, Traces.CORRELATION_HEADER);
            copy(request, headers, "traceparent");
        }
        String correlationId = MDC.get(Traces.CORRELATION_MDC);
        if (correlationId != null && !headers.containsKey(Traces.CORRELATION_HEADER)) {
            headers.set(Traces.CORRELATION_HEADER, correlationId);
        }
        String bookingId = MDC.get(Traces.BOOKING_MDC);
        if (bookingId != null) {
            headers.set(Traces.BOOKING_HEADER, bookingId);
        }
        return headers;
    }

    private void copy(HttpServletRequest request, HttpHeaders headers, String name) {
        String value = request.getHeader(name);
        if (value != null && !value.isBlank()) {
            headers.set(name, value);
        }
    }

    private Throwable unwrap(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof RoomConflictException
                    || current instanceof NotFoundException
                    || current instanceof BadRequestException
                    || current instanceof HotelTransientException
                    || current instanceof TimeoutException) {
                return current;
            }
            if (current.getCause() == null || current.getCause() == current) {
                return current;
            }
            current = current.getCause();
        }
        return exception;
    }

    private record ConfirmBody(java.util.UUID requestId, Long bookingId, LocalDate startDate, LocalDate endDate) {
    }

    private record ReleaseBody(java.util.UUID requestId, Long bookingId) {
    }
}
