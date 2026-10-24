package com.example.booking.exception;

import org.springframework.http.HttpStatus;

public class HotelTransientException extends ApiException {

    public HotelTransientException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message);
    }

    public HotelTransientException(String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message);
        initCause(cause);
    }
}
