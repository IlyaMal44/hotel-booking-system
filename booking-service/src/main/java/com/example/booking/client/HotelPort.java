package com.example.booking.client;

import com.example.booking.dto.ConfirmCommand;
import com.example.booking.dto.ConfirmView;
import com.example.booking.dto.ReleaseCommand;
import com.example.booking.dto.RoomView;

import java.time.LocalDate;
import java.util.List;

public interface HotelPort {

    List<RoomView> recommend(Long hotelId, LocalDate startDate, LocalDate endDate);

    ConfirmView confirm(ConfirmCommand command);

    void release(ReleaseCommand command);
}
