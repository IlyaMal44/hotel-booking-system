package com.example.hotel.dto;

public record RoomResponse(
        /** id номера */
        Long id,
        /** id отеля */
        Long hotelId,
        /** Название отеля */
        String hotelName,
        /** Номер комнаты */
        String number,
        /** true - номер в эксплуатации, даты тут не учитываются */
        boolean available,
        /** Сколько раз номер бронировали */
        int timesBooked,
        /** Вместимость */
        Integer capacity,
        /** Этаж */
        Integer floor
) {
}
