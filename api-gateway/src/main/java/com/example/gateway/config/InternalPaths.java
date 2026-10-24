package com.example.gateway.config;

public final class InternalPaths {

    private InternalPaths() {
    }

    public static boolean isBlocked(String path) {
        return path.endsWith("/confirm-availability") || path.endsWith("/release");
    }
}
