package com.example.booking.service;

import com.example.booking.client.HotelPort;
import com.example.booking.dto.CreateBookingRequest;
import com.example.booking.dto.RoomView;
import com.example.booking.exception.BadRequestException;
import com.example.booking.exception.ConflictException;
import com.example.booking.repository.BookingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class RoomSelectionService {

    private final HotelPort hotelPort;
    private final BookingRepository bookingRepository;

    public RoomSelectionService(HotelPort hotelPort, BookingRepository bookingRepository) {
        this.hotelPort = hotelPort;
        this.bookingRepository = bookingRepository;
    }

    /** При автоподборе берет свободные номера по возрастанию timesBooked, иначе один roomId */
    public List<Long> candidates(CreateBookingRequest request) {
        if (!Boolean.TRUE.equals(request.autoSelect())) {
            if (request.roomId() == null) {
                throw new BadRequestException("roomId is required when autoSelect is false");
            }
            return List.of(request.roomId());
        }
        List<RoomView> recommended = hotelPort.recommend(request.hotelId(), request.startDate(), request.endDate());
        List<Long> ids = recommended.stream()
                .map(RoomView::id)
                .filter(roomId -> !bookingRepository.existsOverlap(roomId, request.startDate(), request.endDate(), -1L))
                .toList();
        log.info("auto-select requestId={} orderedRoomIds={}", request.requestId(), ids);
        if (ids.isEmpty()) {
            throw new ConflictException("No available room for the selected dates");
        }
        return ids;
    }

    /** Отдает номера отеля без тех, что уже заняты локальными бронями на эти даты */
    public List<RoomView> recommend(CreateBookingRequest request) {
        return hotelPort.recommend(request.hotelId(), request.startDate(), request.endDate()).stream()
                .filter(room -> !bookingRepository.existsOverlap(room.id(), request.startDate(), request.endDate(), -1L))
                .toList();
    }
}
