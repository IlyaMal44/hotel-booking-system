package com.example.booking.dto;

public record BookingCreateResult(
        /** Сама бронь, которую отдаем клиенту */
        BookingResponse booking,
        /** true, если это повтор уже обработанного requestId */
        boolean replay
) {
}
