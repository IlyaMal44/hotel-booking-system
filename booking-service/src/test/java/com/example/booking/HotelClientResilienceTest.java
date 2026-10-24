package com.example.booking;

import com.example.booking.client.HotelClient;
import com.example.booking.dto.ConfirmCommand;
import com.example.booking.exception.HotelTransientException;
import com.example.booking.exception.RoomConflictException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("resilience")
class HotelClientResilienceTest {

    private static final MockWebServer SERVER = new MockWebServer();

    static {
        try {
            SERVER.start();
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.hotel.direct-base-url", () -> SERVER.url("/").toString());
    }

    @Autowired
    private HotelClient hotelClient;

    @AfterAll
    static void stop() throws Exception {
        SERVER.shutdown();
    }

    @Test
    @DisplayName("Два ответа 500, третий успешный - вызов повторяется и в итоге проходит")
    void retriesTransientErrorsAndThenSucceeds() {
        SERVER.enqueue(new MockResponse().setResponseCode(500).setBody("down"));
        SERVER.enqueue(new MockResponse().setResponseCode(500).setBody("down"));
        SERVER.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("""
                        {"roomId":1,"requestId":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa","bookingId":1,"timesBooked":2,"locked":true}
                        """));
        int before = SERVER.getRequestCount();

        var response = hotelClient.confirm(command());

        assertThat(response.locked()).isTrue();
        assertThat(SERVER.getRequestCount() - before).isEqualTo(3);
    }

    @Test
    @DisplayName("Отель молчит дольше таймаута - после трех попыток отдаем ошибку")
    void timeoutStopsAfterConfiguredAttempts() {
        for (int attempt = 0; attempt < 3; attempt++) {
            SERVER.enqueue(new MockResponse().setHeadersDelay(2, TimeUnit.SECONDS).setResponseCode(200).setBody("{}"));
        }
        int before = SERVER.getRequestCount();

        assertThatThrownBy(() -> hotelClient.confirm(command())).isInstanceOf(HotelTransientException.class);
        assertThat(SERVER.getRequestCount() - before).isEqualTo(3);
    }

    @Test
    @DisplayName("409 не повторяем, это занятый номер, а не сбой сети")
    void conflictIsNotRetried() {
        SERVER.enqueue(new MockResponse().setResponseCode(409).setBody("taken"));
        int before = SERVER.getRequestCount();

        assertThatThrownBy(() -> hotelClient.confirm(command())).isInstanceOf(RoomConflictException.class);
        assertThat(SERVER.getRequestCount() - before).isEqualTo(1);
    }

    private ConfirmCommand command() {
        LocalDate start = LocalDate.now().plusDays(3);
        return new ConfirmCommand(1L, UUID.randomUUID(), 1L, start, start.plusDays(1));
    }
}
