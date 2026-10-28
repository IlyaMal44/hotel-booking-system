package com.example.common;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class DateRanges {

    public static final int MAX_NIGHTS = 30;

    private DateRanges() {
    }

    /**
     * Проверяет корректность дат для бронирования номера
     * @param start дата заезда (начало периода)
     * @param end   дата выезда (конец периода)
     */
    public static void requireStay(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Нужны startDate и endDate");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("endDate должен быть позже startDate");
        }
        if (start.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("startDate не может быть в прошлом");
        }
        long nights = ChronoUnit.DAYS.between(start, end);
        if (nights > MAX_NIGHTS) {
            throw new IllegalArgumentException("Нельзя бронировать дольше " + MAX_NIGHTS + " ночей");
        }
    }
}
