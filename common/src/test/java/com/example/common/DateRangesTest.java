package com.example.common;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DateRangesTest {

    @Test
    void acceptsFutureStay() {
        LocalDate start = LocalDate.now().plusDays(1);
        assertDoesNotThrow(() -> DateRanges.requireStay(start, start.plusDays(2)));
    }

    @Test
    void rejectsEndBeforeStart() {
        LocalDate start = LocalDate.now().plusDays(3);
        assertThrows(IllegalArgumentException.class, () -> DateRanges.requireStay(start, start));
    }

    @Test
    void rejectsPastStart() {
        assertThrows(IllegalArgumentException.class,
                () -> DateRanges.requireStay(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)));
    }

    @Test
    void rejectsStayLongerThanThirtyNights() {
        LocalDate start = LocalDate.now().plusDays(1);
        assertThrows(IllegalArgumentException.class, () -> DateRanges.requireStay(start, start.plusDays(31)));
    }
}
