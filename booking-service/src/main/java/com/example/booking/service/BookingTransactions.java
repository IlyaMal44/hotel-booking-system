package com.example.booking.service;

import com.example.booking.domain.Booking;
import com.example.booking.domain.User;
import com.example.booking.dto.CreateBookingRequest;
import com.example.booking.enums.BookingStatus;
import com.example.booking.exception.ConflictException;
import com.example.booking.exception.NotFoundException;
import com.example.booking.repository.BookingRepository;
import com.example.booking.repository.RoomGuardRepository;
import com.example.booking.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class BookingTransactions {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final RoomGuardRepository roomGuardRepository;
    private final RoomGuardCoordinator roomGuardCoordinator;

    public BookingTransactions(BookingRepository bookingRepository,
                               UserRepository userRepository,
                               RoomGuardRepository roomGuardRepository,
                               RoomGuardCoordinator roomGuardCoordinator) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.roomGuardRepository = roomGuardRepository;
        this.roomGuardCoordinator = roomGuardCoordinator;
    }

    @Transactional
    /** Под блокировкой номера пишет или обновляет PENDING, если даты свободны */
    public Booking assignRoom(Long bookingId, String username, CreateBookingRequest request, Long roomId) {
        roomGuardCoordinator.ensure(roomId);
        roomGuardRepository.lockByRoomId(roomId).orElseThrow(() -> new ConflictException("Не удалось заблокировать номер"));
        long excludeId = bookingId == null ? -1L : bookingId;
        if (bookingRepository.existsOverlap(roomId, request.startDate(), request.endDate(), excludeId)) {
            throw new ConflictException("Номер уже занят на эти даты");
        }
        if (bookingId == null) {
            User user = userRepository.findByUsername(username).orElseThrow(() -> new NotFoundException("Пользователь не найден"));
            Booking booking = new Booking();
            booking.setUser(user);
            booking.setRoomId(roomId);
            booking.setStartDate(request.startDate());
            booking.setEndDate(request.endDate());
            booking.setStatus(BookingStatus.PENDING);
            booking.setRequestId(request.requestId().toString());
            booking.setCreatedAt(Instant.now());
            booking.setUpdatedAt(booking.getCreatedAt());
            try {
                return bookingRepository.saveAndFlush(booking);
            } catch (DataIntegrityViolationException exception) {
                throw new ConflictException("Этот запрос уже обрабатывается");
            }
        }
        Booking booking = bookingRepository.findWithUserById(bookingId).orElseThrow(() -> new NotFoundException("Бронь не найдена"));
        booking.setRoomId(roomId);
        booking.setStatus(BookingStatus.PENDING);
        booking.setUpdatedAt(Instant.now());
        return booking;
    }

    @Transactional
    /** Переводит PENDING в CONFIRMED после ответа отеля */
    public Booking markConfirmed(Long bookingId) {
        Booking booking = required(bookingId);
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            return booking;
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ConflictException("Нельзя подтвердить бронь из статуса " + booking.getStatus());
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setUpdatedAt(Instant.now());
        return booking;
    }

    @Transactional
    /** Переводит бронь в CANCELLED и сразу коммитит это состояние */
    public Booking markCancelled(Long bookingId) {
        Booking booking = required(bookingId);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return booking;
        }
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setUpdatedAt(Instant.now());
        return booking;
    }

    @Transactional(readOnly = true)
    /** Загружает бронь вместе с пользователем или кидает 404 */
    public Booking required(Long bookingId) {
        return bookingRepository.findWithUserById(bookingId).orElseThrow(() -> new NotFoundException("Бронь не найдена"));
    }
}
