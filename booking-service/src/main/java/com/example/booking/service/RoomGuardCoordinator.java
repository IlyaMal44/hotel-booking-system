package com.example.booking.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class RoomGuardCoordinator {

    private final RoomGuardWriter roomGuardWriter;

    public RoomGuardCoordinator(RoomGuardWriter roomGuardWriter) {
        this.roomGuardWriter = roomGuardWriter;
    }

    /** Гарантирует строку-замок по номеру, гонка на вставке тут не роняет основную транзакцию */
    public void ensure(Long roomId) {
        try {
            roomGuardWriter.insertIfAbsent(roomId);
        } catch (DataIntegrityViolationException ignored) {
            // Another transaction created the guard row first.
        }
    }
}
