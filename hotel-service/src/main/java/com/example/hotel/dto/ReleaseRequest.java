package com.example.hotel.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReleaseRequest(
        /** Ключ блокировки, по нему ищем слот */
        @NotNull UUID requestId,
        /** Бронь, для логов, на поиск слота не влияет */
        Long bookingId
) {
}
