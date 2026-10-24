package com.example.hotel.repository;

import com.example.hotel.domain.RoomHold;
import com.example.hotel.enums.HoldStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RoomHoldRepository extends JpaRepository<RoomHold, Long> {

    @EntityGraph(attributePaths = "room")
    Optional<RoomHold> findByRequestId(String requestId);

    boolean existsByRoomIdAndStatus(Long roomId, HoldStatus status);

    @Query("""
            select count(h) > 0 from RoomHold h
            where h.room.id = :roomId
              and h.status = com.example.hotel.enums.HoldStatus.ACTIVE
              and h.startDate < :endDate
              and h.endDate > :startDate
            """)
    boolean existsActiveOverlap(@Param("roomId") Long roomId,
                                @Param("startDate") LocalDate startDate,
                                @Param("endDate") LocalDate endDate);

    @Query("""
            select h.room.id, count(h) from RoomHold h
            where h.status = com.example.hotel.enums.HoldStatus.ACTIVE
            group by h.room.id
            """)
    List<Object[]> countActiveByRoom();
}
