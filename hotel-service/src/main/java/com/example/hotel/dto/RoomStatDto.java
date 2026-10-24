package com.example.hotel.dto;

public record RoomStatDto(
        /** id номера */
        long roomId,
        /** id отеля */
        long hotelId,
        /** Название отеля */
        String hotelName,
        /** Номер комнаты */
        String number,
        /** true - номер в эксплуатации */
        boolean available,
        /** Сколько раз номер бронировали */
        int timesBooked,
        /** Сколько активных блокировок висит на номере сейчас */
        long activeHolds
) {
}
