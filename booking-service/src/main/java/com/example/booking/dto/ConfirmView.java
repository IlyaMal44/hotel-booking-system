package com.example.booking.dto;

public record ConfirmView(
        /** Номер, по которому ответил hotel-service */
        Long roomId,
        /** requestId, с которым прошла блокировка */
        String requestId,
        /** id брони */
        Long bookingId,
        /** Сколько раз номер уже бронировали, после этой блокировки */
        int timesBooked,
        /** true, если слот сейчас удерживается */
        boolean locked
) {
}
