package com.example.hotel.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record RoomPatchRequest(
        /** Новый номер комнаты, null - не менять */
        @Size(max = 20) String number,
        /** Вывести номер в работу или на ремонт */
        Boolean available,
        /** Новая вместимость */
        @Min(1) @Max(10) Integer capacity,
        /** Новый этаж */
        @Min(0) @Max(200) Integer floor
) {
}
