package com.example.hotel.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record ConfirmAvailabilityRequest(
        /** Ключ идемпотентности, повтор не увеличивает timesBooked второй раз */
        @NotNull UUID requestId,
        /** id брони из booking-service */
        @NotNull Long bookingId,
        /** Дата заезда */
        @NotNull LocalDate startDate,
        /** Дата выезда, этот день в проживание не входит */
        @NotNull LocalDate endDate
) {
}
