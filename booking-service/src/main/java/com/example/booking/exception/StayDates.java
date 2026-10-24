package com.example.booking.exception;

import com.example.common.DateRanges;

import java.time.LocalDate;

public final class StayDates {

    private StayDates() {
    }

    public static void check(LocalDate start, LocalDate end) {
        try {
            DateRanges.requireStay(start, end);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException(exception.getMessage());
        }
    }
}
