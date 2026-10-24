package com.example.booking.repository;

import com.example.booking.domain.RoomGuard;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RoomGuardRepository extends JpaRepository<RoomGuard, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from RoomGuard g where g.roomId = :roomId")
    Optional<RoomGuard> lockByRoomId(@Param("roomId") Long roomId);
}
