package com.example.hotel.service;

import com.example.hotel.dto.RoomPatchRequest;
import com.example.hotel.dto.RoomRequest;
import com.example.hotel.dto.RoomResponse;

public interface RoomCommandService {

    RoomResponse create(RoomRequest request);

    RoomResponse update(Long id, RoomPatchRequest request);

    void delete(Long id);
}
