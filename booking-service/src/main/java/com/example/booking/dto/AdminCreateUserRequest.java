package com.example.booking.dto;

import com.example.booking.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminCreateUserRequest(
        /** Логин, должен быть уникальным */
        @NotBlank @Size(min = 3, max = 50) String username,
        /** Пароль, в базе хранится уже хеш */
        @NotBlank @Size(min = 6, max = 100) String password,
        /** Роль, которую выдает админ */
        @NotNull Role role
) {
}
