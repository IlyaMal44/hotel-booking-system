package com.example.hotel.service.impl;

import com.example.hotel.domain.Room;
import com.example.hotel.dto.OccupancyStatisticsResponse;
import com.example.hotel.dto.RoomStatDto;
import com.example.hotel.repository.RoomHoldRepository;
import com.example.hotel.repository.RoomRepository;
import com.example.hotel.service.OccupancyStatisticsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OccupancyStatisticsServiceImpl implements OccupancyStatisticsService {

    private final RoomRepository roomRepository;
    private final RoomHoldRepository roomHoldRepository;

    public OccupancyStatisticsServiceImpl(RoomRepository roomRepository, RoomHoldRepository roomHoldRepository) {
        this.roomRepository = roomRepository;
        this.roomHoldRepository = roomHoldRepository;
    }

    @Override
    @Transactional(readOnly = true)
    /** Считает загруженность номеров: простой, среднее и самый занятый номер */
    public OccupancyStatisticsResponse statistics(Long hotelId) {
        Map<Long, Long> activeHolds = new HashMap<>();
        for (Object[] row : roomHoldRepository.countActiveByRoom()) {
            activeHolds.put((Long) row[0], (Long) row[1]);
        }
        List<RoomStatDto> rooms = roomRepository.findAll().stream()
                .filter(room -> hotelId == null || room.getHotel().getId().equals(hotelId))
                .map(room -> toStat(room, activeHolds.getOrDefault(room.getId(), 0L)))
                .sorted(Comparator.comparingInt(RoomStatDto::timesBooked).reversed().thenComparing(RoomStatDto::roomId))
                .toList();
        long available = rooms.stream().filter(RoomStatDto::available).count();
        long idle = rooms.stream().filter(room -> room.available() && room.timesBooked() == 0).count();
        long holds = rooms.stream().mapToLong(RoomStatDto::activeHolds).sum();
        double average = rooms.isEmpty() ? 0 : rooms.stream().mapToInt(RoomStatDto::timesBooked).average().orElse(0);
        int max = rooms.stream().mapToInt(RoomStatDto::timesBooked).max().orElse(0);
        RoomStatDto busiest = rooms.stream().max(Comparator.comparingInt(RoomStatDto::timesBooked)).orElse(null);
        return new OccupancyStatisticsResponse(
                rooms.size(),
                available,
                rooms.size() - available,
                idle,
                holds,
                average,
                max,
                busiest,
                rooms
        );
    }

    private RoomStatDto toStat(Room room, long activeHolds) {
        return new RoomStatDto(
                room.getId(),
                room.getHotel().getId(),
                room.getHotel().getName(),
                room.getNumber(),
                room.isAvailable(),
                room.getTimesBooked(),
                activeHolds
        );
    }
}
