package com.example.booking.config;

import com.example.common.Traces;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String correlationId = request.getHeader(Traces.CORRELATION_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        String bookingId = request.getHeader(Traces.BOOKING_HEADER);
        MDC.put(Traces.CORRELATION_MDC, correlationId);
        if (bookingId != null && !bookingId.isBlank()) {
            MDC.put(Traces.BOOKING_MDC, bookingId);
        }
        response.setHeader(Traces.CORRELATION_HEADER, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(Traces.CORRELATION_MDC);
            MDC.remove(Traces.BOOKING_MDC);
        }
    }
}
