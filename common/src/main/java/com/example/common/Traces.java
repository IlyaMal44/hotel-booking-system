package com.example.common;

import org.slf4j.MDC;

public final class Traces {

    public static final String CORRELATION_HEADER = "X-Correlation-Id";
    public static final String BOOKING_HEADER = "X-Booking-Id";
    public static final String CORRELATION_MDC = "correlationId";
    public static final String BOOKING_MDC = "bookingId";
    public static final String TRACE_MDC = "traceId";

    private Traces() {
    }

    /**
     * Возвращает текущий идентификатор трассировки из контекста потока (MDC)
     */
    public static String currentTraceId() {
        String traceId = MDC.get(TRACE_MDC);
        if (traceId != null && !traceId.isBlank()) {
            return traceId;
        }
        return MDC.get(CORRELATION_MDC);
    }
}
