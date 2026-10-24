package com.example.booking.mapper;

import com.example.booking.domain.Booking;
import com.example.booking.domain.User;
import com.example.booking.dto.BookingResponse;
import com.example.booking.dto.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    UserResponse toResponse(User user);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    BookingResponse toResponse(Booking booking);
}
