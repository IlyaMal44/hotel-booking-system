package com.example.common;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public final class JwtKeys {

    private JwtKeys() {
    }

    public static SecretKey hmac(String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalArgumentException("JWT-секрет короче 32 байт");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
}
