package com.example.hotel.dto;

public record ConfirmAvailabilityResponse(
        /** Номер, который заблокировали */
        Long roomId,
        /** requestId этой блокировки */
        String requestId,
        /** Бронь, под которую держим слот */
        Long bookingId,
        /** Счетчик бронирований после операции */
        int timesBooked,
        /** true, если слот активен */
        boolean locked
) {
}
