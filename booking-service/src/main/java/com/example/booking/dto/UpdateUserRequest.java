package com.example.booking.dto;

import com.example.booking.enums.Role;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        /** Кого обновляем */
        @NotNull Long id,
        /** Новый пароль, если поле пустое - пароль не трогаем */
        @Size(min = 6, max = 100) String password,
        /** Новая роль, если null - роль не меняется */
        Role role
) {
}
