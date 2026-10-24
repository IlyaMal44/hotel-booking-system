package com.example.booking.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateBookingRequest(
        /** Ключ идемпотентности: повтор с тем же значением не создает вторую бронь */
        @NotNull UUID requestId,
        /** Дата заезда, включительно */
        @NotNull LocalDate startDate,
        /** Дата выезда, этот день в проживание не входит */
        @NotNull LocalDate endDate,
        /** Номер, который бронируют; при autoSelect=true поле игнорируется */
        Long roomId,
        /** Отель, внутри которого подбирать номер при автоподборе */
        Long hotelId,
        /** true - номер выбирает сервис по возрастанию timesBooked */
        @NotNull Boolean autoSelect
) {
}
