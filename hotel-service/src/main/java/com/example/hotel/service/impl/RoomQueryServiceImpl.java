package com.example.hotel.service.impl;

import com.example.hotel.dto.RoomResponse;
import com.example.hotel.exception.ApiExceptionHandler;
import com.example.hotel.exception.BadRequestException;
import com.example.hotel.exception.NotFoundException;
import com.example.hotel.mapper.HotelMapper;
import com.example.hotel.repository.RoomRepository;
import com.example.hotel.repository.RoomSpecifications;
import com.example.hotel.service.RoomQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class RoomQueryServiceImpl implements RoomQueryService {

    private final RoomRepository roomRepository;
    private final HotelMapper hotelMapper;

    public RoomQueryServiceImpl(RoomRepository roomRepository, HotelMapper hotelMapper) {
        this.roomRepository = roomRepository;
        this.hotelMapper = hotelMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponse get(Long id) {
        return hotelMapper.toResponse(roomRepository.findWithHotelById(id)
                .orElseThrow(() -> new NotFoundException("Room not found")));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RoomResponse> list(Long hotelId, Boolean available, Pageable pageable) {
        Boolean operational = available == null ? Boolean.TRUE : available;
        return roomRepository.findAll(RoomSpecifications.filter(hotelId, operational), pageable)
                .map(hotelMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> recommend(Long hotelId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return roomRepository.findAvailableOrderByTimesBooked(hotelId).stream()
                    .map(hotelMapper::toResponse)
                    .toList();
        }
        if (startDate == null || endDate == null) {
            throw new BadRequestException("startDate and endDate must be provided together");
        }
        ApiExceptionHandler.checkStay(startDate, endDate);
        return roomRepository.findAvailableForDates(hotelId, startDate, endDate).stream()
                .map(hotelMapper::toResponse)
                .toList();
    }
}
