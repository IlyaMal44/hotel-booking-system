package com.example.common;

import java.time.Instant;

public final class ErrorResponses {

    private ErrorResponses() {
    }

    public static ErrorDto of(int status, String error, String message, String path) {
        return new ErrorDto(Instant.now(), status, error, message, path, Traces.currentTraceId());
    }
}
