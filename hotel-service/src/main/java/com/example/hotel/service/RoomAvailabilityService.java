package com.example.hotel.service;

import com.example.hotel.dto.ConfirmAvailabilityRequest;
import com.example.hotel.dto.ConfirmAvailabilityResponse;
import com.example.hotel.dto.ReleaseRequest;
import com.example.hotel.dto.ReleaseResponse;

public interface RoomAvailabilityService {

    ConfirmAvailabilityResponse confirm(Long roomId, ConfirmAvailabilityRequest request);

    ReleaseResponse release(Long roomId, ReleaseRequest request);
}
