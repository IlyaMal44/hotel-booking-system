package com.example.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DateRangesTest {

    @Test
    @DisplayName("Будущий заезд короче 30 ночей проходит")
    void acceptsFutureStay() {
        LocalDate start = LocalDate.now().plusDays(1);
        assertDoesNotThrow(() -> DateRanges.requireStay(start, start.plusDays(2)));
    }

    @Test
    @DisplayName("Выезд в день заезда не принимаем")
    void rejectsEndBeforeStart() {
        LocalDate start = LocalDate.now().plusDays(3);
        assertThrows(IllegalArgumentException.class, () -> DateRanges.requireStay(start, start));
    }

    @Test
    @DisplayName("Заезд в прошлом отклоняем")
    void rejectsPastStart() {
        assertThrows(IllegalArgumentException.class,
                () -> DateRanges.requireStay(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)));
    }

    @Test
    @DisplayName("Дольше 30 ночей жить нельзя")
    void rejectsStayLongerThanThirtyNights() {
        LocalDate start = LocalDate.now().plusDays(1);
        assertThrows(IllegalArgumentException.class, () -> DateRanges.requireStay(start, start.plusDays(31)));
    }
}
