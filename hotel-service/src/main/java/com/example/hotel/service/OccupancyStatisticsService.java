package com.example.hotel.service;

import com.example.hotel.dto.OccupancyStatisticsResponse;

public interface OccupancyStatisticsService {

    OccupancyStatisticsResponse statistics(Long hotelId);
}
