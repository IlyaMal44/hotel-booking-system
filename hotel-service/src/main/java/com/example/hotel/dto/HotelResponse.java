package com.example.hotel.dto;

public record HotelResponse(
        /** id отеля */
        Long id,
        /** Название */
        String name,
        /** Адрес */
        String address,
        /** Описание */
        String description,
        /** Число звезд */
        Integer stars
) {
}
