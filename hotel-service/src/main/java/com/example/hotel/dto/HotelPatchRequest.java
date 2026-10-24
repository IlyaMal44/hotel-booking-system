package com.example.hotel.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record HotelPatchRequest(
        /** Новое название, null - не менять */
        @Size(max = 120) String name,
        /** Новый адрес, null - не менять */
        @Size(max = 250) String address,
        /** Новое описание, null - не менять */
        @Size(max = 500) String description,
        /** Новое число звезд, от 1 до 5 */
        @Min(1) @Max(5) Integer stars
) {
}
