package com.example.hotel.service.impl;

import com.example.hotel.domain.Hotel;
import com.example.hotel.dto.HotelPatchRequest;
import com.example.hotel.dto.HotelRequest;
import com.example.hotel.dto.HotelResponse;
import com.example.hotel.exception.BadRequestException;
import com.example.hotel.exception.ConflictException;
import com.example.hotel.exception.NotFoundException;
import com.example.hotel.mapper.HotelMapper;
import com.example.hotel.repository.HotelRepository;
import com.example.hotel.repository.HotelSpecifications;
import com.example.hotel.repository.RoomRepository;
import com.example.hotel.service.HotelService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class HotelServiceImpl implements HotelService {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final HotelMapper hotelMapper;

    public HotelServiceImpl(HotelRepository hotelRepository, RoomRepository roomRepository, HotelMapper hotelMapper) {
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
        this.hotelMapper = hotelMapper;
    }

    @Override
    @Transactional
    /** Сохраняет новый отель */
    public HotelResponse create(HotelRequest request) {
        Hotel hotel = new Hotel();
        hotel.setName(request.name().trim());
        hotel.setAddress(request.address().trim());
        hotel.setDescription(request.description());
        hotel.setStars(request.stars());
        hotel.setCreatedAt(Instant.now());
        return hotelMapper.toResponse(hotelRepository.save(hotel));
    }

    @Override
    @Transactional(readOnly = true)
    /** Возвращает отель по id */
    public HotelResponse get(Long id) {
        return hotelMapper.toResponse(require(id));
    }

    @Override
    @Transactional(readOnly = true)
    /** Список отелей с фильтром по названию, адресу и звездам */
    public Page<HotelResponse> list(String name, String address, Integer minStars, Pageable pageable) {
        return hotelRepository.findAll(HotelSpecifications.filter(name, address, minStars), pageable)
                .map(hotelMapper::toResponse);
    }

    @Override
    @Transactional
    /** Обновляет только те поля отеля, которые пришли в запросе */
    public HotelResponse update(Long id, HotelPatchRequest request) {
        if (request.name() == null && request.address() == null && request.description() == null && request.stars() == null) {
            throw new BadRequestException("Нужно передать хотя бы одно поле");
        }
        Hotel hotel = require(id);
        if (request.name() != null) {
            hotel.setName(request.name().trim());
        }
        if (request.address() != null) {
            hotel.setAddress(request.address().trim());
        }
        if (request.description() != null) {
            hotel.setDescription(request.description());
        }
        if (request.stars() != null) {
            hotel.setStars(request.stars());
        }
        return hotelMapper.toResponse(hotel);
    }

    @Override
    @Transactional
    /** Удаляет отель, если в нем не осталось номеров */
    public void delete(Long id) {
        if (!hotelRepository.existsById(id)) {
            throw new NotFoundException("Отель не найден");
        }
        if (roomRepository.existsByHotelId(id)) {
            throw new ConflictException("В отеле еще есть номера");
        }
        hotelRepository.deleteById(id);
    }

    private Hotel require(Long id) {
        return hotelRepository.findById(id).orElseThrow(() -> new NotFoundException("Отель не найден"));
    }
}
