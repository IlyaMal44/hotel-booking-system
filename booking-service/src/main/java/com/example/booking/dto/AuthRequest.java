package com.example.booking.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthRequest(
        /** Логин */
        @NotBlank String username,
        /** Пароль в открытом виде, сверяем с хешем */
        @NotBlank String password
) {
}
