package com.example.hotel.mapper;

import com.example.hotel.domain.Hotel;
import com.example.hotel.domain.Room;
import com.example.hotel.dto.HotelResponse;
import com.example.hotel.dto.RoomResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface HotelMapper {

    HotelResponse toResponse(Hotel hotel);

    @Mapping(target = "hotelId", source = "hotel.id")
    @Mapping(target = "hotelName", source = "hotel.name")
    RoomResponse toResponse(Room room);
}
