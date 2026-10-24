package com.example.booking.service;

import com.example.booking.domain.RoomGuard;
import com.example.booking.repository.RoomGuardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomGuardWriter {

    private final RoomGuardRepository roomGuardRepository;

    public RoomGuardWriter(RoomGuardRepository roomGuardRepository) {
        this.roomGuardRepository = roomGuardRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    /** Вставляет room_guards в отдельной транзакции, если строки еще нет */
    public void insertIfAbsent(Long roomId) {
        if (!roomGuardRepository.existsById(roomId)) {
            roomGuardRepository.saveAndFlush(new RoomGuard(roomId));
        }
    }
}
