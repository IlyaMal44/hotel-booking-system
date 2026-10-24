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
    public Booking assignRoom(Long bookingId, String username, CreateBookingRequest request, Long roomId) {
        roomGuardCoordinator.ensure(roomId);
        roomGuardRepository.lockByRoomId(roomId).orElseThrow(() -> new ConflictException("Room lock is unavailable"));
        long excludeId = bookingId == null ? -1L : bookingId;
        if (bookingRepository.existsOverlap(roomId, request.startDate(), request.endDate(), excludeId)) {
            throw new ConflictException("Room is already booked for these dates");
        }
        if (bookingId == null) {
            User user = userRepository.findByUsername(username).orElseThrow(() -> new NotFoundException("User not found"));
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
                throw new ConflictException("Request is already being processed");
            }
        }
        Booking booking = bookingRepository.findWithUserById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        booking.setRoomId(roomId);
        booking.setStatus(BookingStatus.PENDING);
        booking.setUpdatedAt(Instant.now());
        return booking;
    }

    @Transactional
    public Booking markConfirmed(Long bookingId) {
        Booking booking = required(bookingId);
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            return booking;
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ConflictException("Booking cannot be confirmed from status " + booking.getStatus());
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setUpdatedAt(Instant.now());
        return booking;
    }

    @Transactional
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
    public Booking required(Long bookingId) {
        return bookingRepository.findWithUserById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
    }
}
