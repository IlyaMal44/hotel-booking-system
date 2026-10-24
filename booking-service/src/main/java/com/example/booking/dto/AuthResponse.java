package com.example.booking.dto;

public record AuthResponse(
        /** JWT, его кладут в Authorization: Bearer */
        String token,
        /** Всегда Bearer */
        String tokenType,
        /** Через сколько секунд токен протухнет, у нас 3600 */
        long expiresInSeconds,
        /** Пользователь, для которого выписали токен */
        UserResponse user
) {
}
