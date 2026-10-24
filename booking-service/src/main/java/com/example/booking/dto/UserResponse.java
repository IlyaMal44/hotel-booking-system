package com.example.booking.dto;

import com.example.booking.enums.Role;

import java.time.Instant;

public record UserResponse(
        /** id пользователя */
        Long id,
        /** Логин */
        String username,
        /** USER или ADMIN */
        Role role,
        /** Когда учетку создали */
        Instant createdAt
) {
}
