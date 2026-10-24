package com.example.booking.service;

import com.example.booking.client.HotelPort;
import com.example.booking.domain.Booking;
import com.example.booking.dto.BookingCreateResult;
import com.example.booking.dto.BookingResponse;
import com.example.booking.dto.ConfirmCommand;
import com.example.booking.dto.CreateBookingRequest;
import com.example.booking.dto.ReleaseCommand;
import com.example.booking.enums.BookingStatus;
import com.example.booking.exception.BadRequestException;
import com.example.booking.exception.ConflictException;
import com.example.booking.exception.HotelTransientException;
import com.example.booking.exception.NotFoundException;
import com.example.booking.exception.RoomConflictException;
import com.example.booking.exception.StayDates;
import com.example.booking.mapper.BookingMapper;
import com.example.booking.repository.BookingRepository;
import com.example.common.Traces;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class BookingSagaService {

    private final BookingRepository bookingRepository;
    private final BookingTransactions transactions;
    private final RoomSelectionService roomSelectionService;
    private final HotelPort hotelPort;
    private final BookingMapper bookingMapper;

    public BookingSagaService(BookingRepository bookingRepository,
                              BookingTransactions transactions,
                              RoomSelectionService roomSelectionService,
                              HotelPort hotelPort,
                              BookingMapper bookingMapper) {
        this.bookingRepository = bookingRepository;
        this.transactions = transactions;
        this.roomSelectionService = roomSelectionService;
        this.hotelPort = hotelPort;
        this.bookingMapper = bookingMapper;
    }

    /** Создает бронь: пишет PENDING, подтверждает номер у отеля и при сбое отменяет */
    public BookingCreateResult create(CreateBookingRequest request, String username) {
        StayDates.check(request.startDate(), request.endDate());
        Optional<Booking> existing = bookingRepository.findByRequestId(request.requestId().toString());
        if (existing.isPresent()) {
            Booking booking = existing.get();
            log.info("bookingId={} requestId={} result=IDEMPOTENT_REPLAY status={}",
                    booking.getId(), booking.getRequestId(), booking.getStatus());
            return new BookingCreateResult(resume(booking), true);
        }
        List<Long> candidates = roomSelectionService.candidates(request);
        Booking booking = null;
        for (Long roomId : candidates) {
            try {
                Long currentId = booking == null ? null : booking.getId();
                booking = transactions.assignRoom(currentId, username, request, roomId);
            } catch (ConflictException exception) {
                log.info("requestId={} roomId={} skipped reason=overlap", request.requestId(), roomId);
                if (!Boolean.TRUE.equals(request.autoSelect())) {
                    throw exception;
                }
                continue;
            }
            MDC.put(Traces.BOOKING_MDC, String.valueOf(booking.getId()));
            try {
                log.info("bookingId={} state=PENDING roomId={} requestId={}", booking.getId(), roomId, request.requestId());
                hotelPort.confirm(new ConfirmCommand(roomId, request.requestId(), booking.getId(), request.startDate(), request.endDate()));
                Booking confirmed = transactions.markConfirmed(booking.getId());
                log.info("bookingId={} transition=PENDING->CONFIRMED roomId={}", confirmed.getId(), roomId);
                return new BookingCreateResult(bookingMapper.toResponse(confirmed), false);
            } catch (RoomConflictException exception) {
                log.info("bookingId={} roomId={} confirm rejected", booking.getId(), roomId);
                if (!Boolean.TRUE.equals(request.autoSelect())) {
                    cancelQuietly(booking.getId());
                    log.info("bookingId={} transition=PENDING->CANCELLED reason=conflict", booking.getId());
                    throw new ConflictException("Room is already booked for these dates");
                }
            } catch (NotFoundException | BadRequestException exception) {
                cancelQuietly(booking.getId());
                log.info("bookingId={} transition=PENDING->CANCELLED reason=rejected", booking.getId());
                throw exception;
            } catch (HotelTransientException exception) {
                cancelQuietly(booking.getId());
                log.warn("bookingId={} transition=PENDING->CANCELLED reason=hotel_unavailable", booking.getId());
                compensate(roomId, request.requestId(), booking.getId());
                throw new HotelTransientException("Booking cancelled: hotel service did not confirm availability", exception);
            } finally {
                MDC.remove(Traces.BOOKING_MDC);
            }
        }
        if (booking != null) {
            cancelQuietly(booking.getId());
            log.info("bookingId={} transition=PENDING->CANCELLED reason=no_room", booking.getId());
        }
        throw new ConflictException("No available room for the selected dates");
    }

    /** Отменяет свою бронь и снимает блокировку номера в hotel-service */
    public BookingResponse cancel(Long id, String username, boolean admin) {
        Booking booking = transactions.required(id);
        assertOwner(booking, username, admin);
        MDC.put(Traces.BOOKING_MDC, String.valueOf(booking.getId()));
        try {
            if (booking.getStatus() != BookingStatus.CANCELLED) {
                BookingStatus previous = booking.getStatus();
                booking = transactions.markCancelled(id);
                log.info("bookingId={} transition={}->CANCELLED reason=user_cancel", id, previous);
            }
            compensate(booking.getRoomId(), UUID.fromString(booking.getRequestId()), booking.getId());
            return bookingMapper.toResponse(transactions.required(id));
        } finally {
            MDC.remove(Traces.BOOKING_MDC);
        }
    }

    /** Повтор того же requestId: возвращает итог или доделывает незавершенный PENDING */
    private BookingResponse resume(Booking booking) {
        MDC.put(Traces.BOOKING_MDC, String.valueOf(booking.getId()));
        try {
            return switch (booking.getStatus()) {
                case CONFIRMED -> bookingMapper.toResponse(booking);
                case CANCELLED -> {
                    compensate(booking.getRoomId(), UUID.fromString(booking.getRequestId()), booking.getId());
                    yield bookingMapper.toResponse(booking);
                }
                case PENDING -> continuePending(booking);
            };
        } finally {
            MDC.remove(Traces.BOOKING_MDC);
        }
    }

    /** Еще раз просит отель подтвердить уже созданный PENDING */
    private BookingResponse continuePending(Booking booking) {
        log.info("bookingId={} state=PENDING result=RESUME requestId={}", booking.getId(), booking.getRequestId());
        try {
            hotelPort.confirm(new ConfirmCommand(
                    booking.getRoomId(),
                    UUID.fromString(booking.getRequestId()),
                    booking.getId(),
                    booking.getStartDate(),
                    booking.getEndDate()));
            Booking confirmed = transactions.markConfirmed(booking.getId());
            log.info("bookingId={} transition=PENDING->CONFIRMED roomId={}", confirmed.getId(), confirmed.getRoomId());
            return bookingMapper.toResponse(confirmed);
        } catch (RoomConflictException exception) {
            cancelQuietly(booking.getId());
            log.info("bookingId={} transition=PENDING->CANCELLED reason=conflict", booking.getId());
            throw new ConflictException("Room is already booked for these dates");
        } catch (HotelTransientException exception) {
            cancelQuietly(booking.getId());
            log.warn("bookingId={} transition=PENDING->CANCELLED reason=hotel_unavailable", booking.getId());
            compensate(booking.getRoomId(), UUID.fromString(booking.getRequestId()), booking.getId());
            throw new HotelTransientException("Booking cancelled: hotel service did not confirm availability", exception);
        }
    }

    /** Снимает блокировку номера, если на этапе confirm она успела встать */
    private void compensate(Long roomId, UUID requestId, Long bookingId) {
        try {
            hotelPort.release(new ReleaseCommand(roomId, requestId, bookingId));
            log.info("bookingId={} compensation=release result=OK roomId={}", bookingId, roomId);
        } catch (HotelTransientException exception) {
            log.error("bookingId={} compensation=release result=FAILED roomId={}", bookingId, roomId, exception);
            throw new HotelTransientException("Booking was cancelled locally, but room release failed", exception);
        }
    }

    /** Переводит бронь в CANCELLED отдельной транзакцией */
    private void cancelQuietly(Long bookingId) {
        transactions.markCancelled(bookingId);
    }

    /** Пускает к брони владельца или админа */
    private void assertOwner(Booking booking, String username, boolean admin) {
        if (!admin && !booking.getUser().getUsername().equals(username)) {
            throw new com.example.booking.exception.ApiException(HttpStatus.FORBIDDEN, "Booking belongs to another user");
        }
    }
}
