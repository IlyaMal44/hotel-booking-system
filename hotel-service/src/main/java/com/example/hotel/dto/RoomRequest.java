package com.example.hotel.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RoomRequest(
        /** В какой отель добавить номер */
        @NotNull Long hotelId,
        /** Номер комнаты, в одном отеле не повторяется */
        @NotBlank @Size(max = 20) String number,
        /** false, если номер сразу заводим как нерабочий */
        @NotNull Boolean available,
        /** Вместимость, можно не указывать */
        @Min(1) @Max(10) Integer capacity,
        /** Этаж, можно не указывать */
        @Min(0) @Max(200) Integer floor
) {
}
