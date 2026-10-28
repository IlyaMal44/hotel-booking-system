package com.example.hotel.exception;

import com.example.common.DateRanges;
import com.example.common.ErrorDto;
import com.example.common.ErrorResponses;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorDto> handleApi(ApiException exception, HttpServletRequest request) {
        return response(exception.getStatus().value(), exception.getStatus().getReasonPhrase(), exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDto> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return response(400, "Bad Request", message, request);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorDto> handleBadBody(Exception exception, HttpServletRequest request) {
        return response(400, "Bad Request", "Тело запроса не разобрать", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorDto> handleDenied(AccessDeniedException exception, HttpServletRequest request) {
        return response(403, "Forbidden", "Недостаточно прав", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unhandled error path={}", request.getRequestURI(), exception);
        return response(500, "Internal Server Error", "Внутренняя ошибка", request);
    }

    public static void checkStay(java.time.LocalDate start, java.time.LocalDate end) {
        try {
            DateRanges.requireStay(start, end);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException(exception.getMessage());
        }
    }

    private ResponseEntity<ErrorDto> response(int status, String error, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(ErrorResponses.of(status, error, message, request.getRequestURI()));
    }
}
