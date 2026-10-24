package com.example.hotel.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HotelRequest(
        /** Название отеля */
        @NotBlank @Size(max = 120) String name,
        /** Адрес */
        @NotBlank @Size(max = 250) String address,
        /** Описание, можно не передавать */
        @Size(max = 500) String description,
        /** Сколько звезд, от 1 до 5 */
        @Min(1) @Max(5) Integer stars
) {
}
