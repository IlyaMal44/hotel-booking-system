package com.example.booking.dto;

import com.example.booking.enums.BookingStatus;

import java.time.Instant;
import java.time.LocalDate;

public record BookingResponse(
        /** id брони */
        Long id,
        /** Кто забронировал */
        Long userId,
        /** Логин владельца */
        String username,
        /** Номер в hotel-service */
        Long roomId,
        /** Дата заезда */
        LocalDate startDate,
        /** Дата выезда, этот день в проживание не входит */
        LocalDate endDate,
        /** PENDING, CONFIRMED или CANCELLED */
        BookingStatus status,
        /** Ключ идемпотентности, с которым бронь создавали */
        String requestId,
        /** Когда бронь впервые записали */
        Instant createdAt
) {
}
