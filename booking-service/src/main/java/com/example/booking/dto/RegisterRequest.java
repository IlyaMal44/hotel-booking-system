package com.example.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        /** Логин, роль всегда USER */
        @NotBlank @Size(min = 3, max = 50) String username,
        /** Пароль, минимум 6 символов */
        @NotBlank @Size(min = 6, max = 100) String password
) {
}
