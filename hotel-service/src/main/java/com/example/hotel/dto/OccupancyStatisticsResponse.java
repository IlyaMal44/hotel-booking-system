package com.example.hotel.dto;

import java.util.List;

public record OccupancyStatisticsResponse(
        /** Всего номеров в выборке */
        long totalRooms,
        /** Номера в эксплуатации */
        long availableRooms,
        /** Номера на ремонте или выведенные */
        long outOfServiceRooms,
        /** Рабочие номера, которые еще ни разу не бронировали */
        long idleRooms,
        /** Сумма активных блокировок */
        long activeHolds,
        /** Среднее timesBooked по номерам выборки */
        double averageTimesBooked,
        /** Максимальный timesBooked */
        int maxTimesBooked,
        /** Номер с самым большим счетчиком */
        RoomStatDto busiest,
        /** Разбивка по каждому номеру */
        List<RoomStatDto> rooms
) {
}
