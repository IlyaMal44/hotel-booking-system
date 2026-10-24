package com.example.booking.dto;

import java.time.LocalDate;
import java.util.UUID;

public record ConfirmCommand(
        /** Номер, который просим заблокировать */
        Long roomId,
        /** Ключ идемпотентности подтверждения */
        UUID requestId,
        /** Уже созданная бронь в статусе PENDING */
        Long bookingId,
        /** Дата заезда */
        LocalDate startDate,
        /** Дата выезда, этот день в проживание не входит */
        LocalDate endDate
) {
}
