package com.example.booking.dto;

import java.util.UUID;

public record ReleaseCommand(
        /** Номер, с которого снимаем блокировку */
        Long roomId,
        /** Тот же requestId, с которым номер блокировали */
        UUID requestId,
        /** Бронь, для которой снимаем слот */
        Long bookingId
) {
}
