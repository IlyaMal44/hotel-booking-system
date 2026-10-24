package com.example.hotel.service.impl;

import com.example.hotel.domain.Hotel;
import com.example.hotel.domain.Room;
import com.example.hotel.dto.RoomPatchRequest;
import com.example.hotel.dto.RoomRequest;
import com.example.hotel.dto.RoomResponse;
import com.example.hotel.enums.HoldStatus;
import com.example.hotel.exception.BadRequestException;
import com.example.hotel.exception.ConflictException;
import com.example.hotel.exception.NotFoundException;
import com.example.hotel.mapper.HotelMapper;
import com.example.hotel.repository.HotelRepository;
import com.example.hotel.repository.RoomHoldRepository;
import com.example.hotel.repository.RoomRepository;
import com.example.hotel.service.RoomCommandService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class RoomCommandServiceImpl implements RoomCommandService {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final RoomHoldRepository roomHoldRepository;
    private final HotelMapper hotelMapper;

    public RoomCommandServiceImpl(RoomRepository roomRepository,
                                  HotelRepository hotelRepository,
                                  RoomHoldRepository roomHoldRepository,
                                  HotelMapper hotelMapper) {
        this.roomRepository = roomRepository;
        this.hotelRepository = hotelRepository;
        this.roomHoldRepository = roomHoldRepository;
        this.hotelMapper = hotelMapper;
    }

    @Override
    @Transactional
    /** Добавляет номер в отель, номер комнаты внутри отеля уникален */
    public RoomResponse create(RoomRequest request) {
        Hotel hotel = hotelRepository.findById(request.hotelId())
                .orElseThrow(() -> new NotFoundException("Hotel not found"));
        Room room = new Room();
        room.setHotel(hotel);
        room.setNumber(request.number().trim());
        room.setAvailable(request.available());
        room.setTimesBooked(0);
        room.setCapacity(request.capacity());
        room.setFloor(request.floor());
        room.setCreatedAt(Instant.now());
        try {
            return hotelMapper.toResponse(roomRepository.saveAndFlush(room));
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Room number already exists in this hotel");
        }
    }

    @Override
    @Transactional
    /** Меняет номер комнаты, доступность, вместимость или этаж */
    public RoomResponse update(Long id, RoomPatchRequest request) {
        if (request.number() == null && request.available() == null && request.capacity() == null && request.floor() == null) {
            throw new BadRequestException("At least one field must be provided");
        }
        Room room = roomRepository.findWithHotelById(id).orElseThrow(() -> new NotFoundException("Room not found"));
        if (request.number() != null) {
            room.setNumber(request.number().trim());
        }
        if (request.available() != null) {
            room.setAvailable(request.available());
        }
        if (request.capacity() != null) {
            room.setCapacity(request.capacity());
        }
        if (request.floor() != null) {
            room.setFloor(request.floor());
        }
        try {
            return hotelMapper.toResponse(roomRepository.saveAndFlush(room));
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Room number already exists in this hotel");
        }
    }

    @Override
    @Transactional
    /** Удаляет номер, если на нем нет активной блокировки */
    public void delete(Long id) {
        if (!roomRepository.existsById(id)) {
            throw new NotFoundException("Room not found");
        }
        if (roomHoldRepository.existsByRoomIdAndStatus(id, HoldStatus.ACTIVE)) {
            throw new ConflictException("Room has an active hold");
        }
        roomRepository.deleteById(id);
    }
}
