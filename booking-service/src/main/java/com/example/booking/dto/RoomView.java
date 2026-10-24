package com.example.booking.dto;

public record RoomView(
        /** id номера в hotel-service */
        Long id,
        /** id отеля */
        Long hotelId,
        /** Название отеля, чтобы не ходить за ним отдельно */
        String hotelName,
        /** Номер комнаты, как на двери */
        String number,
        /** true - номер в эксплуатации, это не занятость по датам */
        boolean available,
        /** Счетчик бронирований, по нему идет автоподбор */
        int timesBooked,
        /** Сколько человек помещается */
        Integer capacity,
        /** Этаж */
        Integer floor
) {
}
