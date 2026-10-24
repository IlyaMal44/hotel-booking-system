package com.example.hotel.service.impl;

import com.example.common.Traces;
import com.example.hotel.domain.Room;
import com.example.hotel.domain.RoomHold;
import com.example.hotel.dto.ConfirmAvailabilityRequest;
import com.example.hotel.dto.ConfirmAvailabilityResponse;
import com.example.hotel.dto.ReleaseRequest;
import com.example.hotel.dto.ReleaseResponse;
import com.example.hotel.enums.HoldStatus;
import com.example.hotel.exception.ApiExceptionHandler;
import com.example.hotel.exception.BadRequestException;
import com.example.hotel.exception.ConflictException;
import com.example.hotel.exception.NotFoundException;
import com.example.hotel.repository.RoomHoldRepository;
import com.example.hotel.repository.RoomRepository;
import com.example.hotel.service.RoomAvailabilityService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
public class RoomAvailabilityServiceImpl implements RoomAvailabilityService {

    private final RoomRepository roomRepository;
    private final RoomHoldRepository roomHoldRepository;
    private final RoomAvailabilityTx availabilityTx;

    public RoomAvailabilityServiceImpl(RoomRepository roomRepository,
                                       RoomHoldRepository roomHoldRepository,
                                       RoomAvailabilityTx availabilityTx) {
        this.roomRepository = roomRepository;
        this.roomHoldRepository = roomHoldRepository;
        this.availabilityTx = availabilityTx;
    }

    @Override
    /** Блокирует номер на даты, повтор того же requestId счетчик не увеличивает */
    public ConfirmAvailabilityResponse confirm(Long roomId, ConfirmAvailabilityRequest request) {
        MDC.put(Traces.BOOKING_MDC, String.valueOf(request.bookingId()));
        try {
            ApiExceptionHandler.checkStay(request.startDate(), request.endDate());
            return roomHoldRepository.findByRequestId(request.requestId().toString())
                    .map(existing -> replay(existing))
                    .orElseGet(() -> confirmNew(roomId, request));
        } finally {
            MDC.remove(Traces.BOOKING_MDC);
        }
    }

    @Override
    /** Снимает блокировку по requestId, повторный вызов ничего не меняет */
    public ReleaseResponse release(Long roomId, ReleaseRequest request) {
        if (request.bookingId() != null) {
            MDC.put(Traces.BOOKING_MDC, String.valueOf(request.bookingId()));
        }
        try {
            return availabilityTx.release(roomId, request.requestId().toString());
        } finally {
            MDC.remove(Traces.BOOKING_MDC);
        }
    }

    /** Создает новую блокировку, а при гонке requestId возвращает уже существующую */
    private ConfirmAvailabilityResponse confirmNew(Long roomId, ConfirmAvailabilityRequest request) {
        try {
            return availabilityTx.lock(roomId, request);
        } catch (DataIntegrityViolationException exception) {
            return roomHoldRepository.findByRequestId(request.requestId().toString())
                    .map(this::replay)
                    .orElseThrow(() -> exception);
        }
    }

    /** Ответ на уже обработанный requestId без второго увеличения timesBooked */
    private ConfirmAvailabilityResponse replay(RoomHold hold) {
        Room room = hold.getRoom();
        log.info("bookingId={} requestId={} roomId={} result=IDEMPOTENT_REPLAY status={}",
                hold.getBookingId(), hold.getRequestId(), room.getId(), hold.getStatus());
        if (hold.getStatus() != HoldStatus.ACTIVE) {
            throw new ConflictException("Availability request was already released");
        }
        return new ConfirmAvailabilityResponse(room.getId(), hold.getRequestId(), hold.getBookingId(), room.getTimesBooked(), true);
    }

    @Slf4j
    @Service
    public static class RoomAvailabilityTx {

        private final RoomRepository roomRepository;
        private final RoomHoldRepository roomHoldRepository;

        public RoomAvailabilityTx(RoomRepository roomRepository, RoomHoldRepository roomHoldRepository) {
            this.roomRepository = roomRepository;
            this.roomHoldRepository = roomHoldRepository;
        }

        @Transactional
        /** Под блокировкой строки номера проверяет пересечение дат и ставит hold */
        public ConfirmAvailabilityResponse lock(Long roomId, ConfirmAvailabilityRequest request) {
            Room room = roomRepository.findByIdForUpdate(roomId)
                    .orElseThrow(() -> new NotFoundException("Room not found"));
            if (!room.isAvailable()) {
                throw new ConflictException("Room is out of service");
            }
            if (roomHoldRepository.existsActiveOverlap(roomId, request.startDate(), request.endDate())) {
                log.info("bookingId={} roomId={} requestId={} result=CONFLICT", request.bookingId(), roomId, request.requestId());
                throw new ConflictException("Room is already booked for these dates");
            }
            RoomHold hold = new RoomHold();
            hold.setRoom(room);
            hold.setBookingId(request.bookingId());
            hold.setRequestId(request.requestId().toString());
            hold.setStartDate(request.startDate());
            hold.setEndDate(request.endDate());
            hold.setStatus(HoldStatus.ACTIVE);
            hold.setCreatedAt(Instant.now());
            roomHoldRepository.saveAndFlush(hold);
            room.setTimesBooked(room.getTimesBooked() + 1);
            log.info("bookingId={} roomId={} requestId={} result=LOCKED timesBooked={} dates={}..{}",
                    request.bookingId(), roomId, request.requestId(), room.getTimesBooked(), request.startDate(), request.endDate());
            return new ConfirmAvailabilityResponse(roomId, hold.getRequestId(), request.bookingId(), room.getTimesBooked(), true);
        }

        @Transactional
        /** Переводит hold в RELEASED и уменьшает timesBooked один раз */
        public ReleaseResponse release(Long roomId, String requestId) {
            RoomHold hold = roomHoldRepository.findByRequestId(requestId).orElse(null);
            if (hold == null || hold.getStatus() == HoldStatus.RELEASED) {
                log.info("bookingId={} roomId={} requestId={} compensation=release result=IDEMPOTENT_NOOP",
                        hold == null ? null : hold.getBookingId(), roomId, requestId);
                return new ReleaseResponse(roomId, requestId, false);
            }
            if (!hold.getRoom().getId().equals(roomId)) {
                throw new BadRequestException("requestId does not belong to this room");
            }
            Room room = roomRepository.findByIdForUpdate(roomId)
                    .orElseThrow(() -> new NotFoundException("Room not found"));
            hold.setStatus(HoldStatus.RELEASED);
            hold.setReleasedAt(Instant.now());
            room.setTimesBooked(Math.max(0, room.getTimesBooked() - 1));
            log.info("bookingId={} roomId={} requestId={} compensation=release result=RELEASED timesBooked={}",
                    hold.getBookingId(), roomId, requestId, room.getTimesBooked());
            return new ReleaseResponse(roomId, requestId, true);
        }
    }
}
