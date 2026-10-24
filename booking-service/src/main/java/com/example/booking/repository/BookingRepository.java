package com.example.booking.repository;

import com.example.booking.domain.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<Booking> findByRequestId(String requestId);

    @EntityGraph(attributePaths = "user")
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findWithUserById(@Param("id") Long id);

    @EntityGraph(attributePaths = "user")
    Page<Booking> findByUserUsername(String username, Pageable pageable);

    boolean existsByUserId(Long userId);

    @Query("""
            select count(b) > 0 from Booking b
            where b.roomId = :roomId
              and b.status in (com.example.booking.enums.BookingStatus.PENDING,
                               com.example.booking.enums.BookingStatus.CONFIRMED)
              and b.id <> :excludeId
              and b.startDate < :endDate
              and b.endDate > :startDate
            """)
    boolean existsOverlap(@Param("roomId") Long roomId,
                          @Param("startDate") LocalDate startDate,
                          @Param("endDate") LocalDate endDate,
                          @Param("excludeId") Long excludeId);
}
