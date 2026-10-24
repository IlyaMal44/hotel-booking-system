package com.example.booking.service;

import com.example.booking.domain.Booking;
import com.example.booking.dto.BookingResponse;
import com.example.booking.dto.CreateBookingRequest;
import com.example.booking.dto.RoomView;
import com.example.booking.exception.ApiException;
import com.example.booking.exception.StayDates;
import com.example.booking.mapper.BookingMapper;
import com.example.booking.repository.BookingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class BookingQueryService {

    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final RoomSelectionService roomSelectionService;

    public BookingQueryService(BookingRepository bookingRepository,
                               BookingMapper bookingMapper,
                               RoomSelectionService roomSelectionService) {
        this.bookingRepository = bookingRepository;
        this.bookingMapper = bookingMapper;
        this.roomSelectionService = roomSelectionService;
    }

    @Transactional(readOnly = true)
    public BookingResponse get(Long id, String username, boolean admin) {
        Booking booking = bookingRepository.findWithUserById(id)
                .orElseThrow(() -> new com.example.booking.exception.NotFoundException("Booking not found"));
        if (!admin && !booking.getUser().getUsername().equals(username)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Booking belongs to another user");
        }
        return bookingMapper.toResponse(booking);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> history(String username, Pageable pageable) {
        return bookingRepository.findByUserUsername(username, pageable).map(bookingMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<RoomView> recommend(LocalDate startDate, LocalDate endDate, Long hotelId) {
        StayDates.check(startDate, endDate);
        return roomSelectionService.recommend(new CreateBookingRequest(null, startDate, endDate, null, hotelId, true));
    }
}
