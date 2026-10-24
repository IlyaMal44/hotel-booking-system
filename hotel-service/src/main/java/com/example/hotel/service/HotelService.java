package com.example.hotel.service;

import com.example.hotel.dto.HotelPatchRequest;
import com.example.hotel.dto.HotelRequest;
import com.example.hotel.dto.HotelResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HotelService {

    HotelResponse create(HotelRequest request);

    HotelResponse get(Long id);

    Page<HotelResponse> list(String name, String address, Integer minStars, Pageable pageable);

    HotelResponse update(Long id, HotelPatchRequest request);

    void delete(Long id);
}
