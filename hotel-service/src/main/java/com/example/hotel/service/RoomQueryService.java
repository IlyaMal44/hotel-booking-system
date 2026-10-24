package com.example.hotel.service;

import com.example.hotel.dto.RoomPatchRequest;
import com.example.hotel.dto.RoomRequest;
import com.example.hotel.dto.RoomResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface RoomQueryService {

    RoomResponse get(Long id);

    Page<RoomResponse> list(Long hotelId, Boolean available, Pageable pageable);

    List<RoomResponse> recommend(Long hotelId, LocalDate startDate, LocalDate endDate);
}
