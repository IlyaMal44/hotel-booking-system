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
            throw new IllegalArgumentException("startDate and endDate are required");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("endDate must be after startDate");
        }
        if (start.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("startDate must not be in the past");
        }
        long nights = ChronoUnit.DAYS.between(start, end);
        if (nights > MAX_NIGHTS) {
            throw new IllegalArgumentException("Stay cannot exceed " + MAX_NIGHTS + " nights");
        }
    }
}
