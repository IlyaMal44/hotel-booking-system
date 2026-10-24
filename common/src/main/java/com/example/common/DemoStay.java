package com.example.common;

import java.time.LocalDate;

public final class DemoStay {

    public static final String REQUEST_ID = "11111111-1111-1111-1111-111111111111";
    public static final long BOOKING_ID = 1L;
    public static final String ROOM_NUMBER = "101";

    private DemoStay() {
    }

    public static LocalDate start() {
        return LocalDate.now().plusDays(60);
    }

    public static LocalDate end() {
        return start().plusDays(4);
    }
}
