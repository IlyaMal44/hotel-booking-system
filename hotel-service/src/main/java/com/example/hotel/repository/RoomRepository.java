package com.example.hotel.repository;

import com.example.hotel.domain.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long>, JpaSpecificationExecutor<Room> {

    boolean existsByHotelId(Long hotelId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = "hotel")
    @Query("select r from Room r where r.id = :id")
    Optional<Room> findWithHotelById(@Param("id") Long id);

    @Query("""
            select r from Room r join fetch r.hotel h
            where r.available = true
              and (:hotelId is null or h.id = :hotelId)
            order by r.timesBooked asc, r.id asc
            """)
    List<Room> findAvailableOrderByTimesBooked(@Param("hotelId") Long hotelId);

    @Query("""
            select r from Room r join fetch r.hotel h
            where r.available = true
              and (:hotelId is null or h.id = :hotelId)
              and not exists (
                    select 1 from RoomHold hold
                    where hold.room = r
                      and hold.status = com.example.hotel.enums.HoldStatus.ACTIVE
                      and hold.startDate < :endDate
                      and hold.endDate > :startDate
              )
            order by r.timesBooked asc, r.id asc
            """)
    List<Room> findAvailableForDates(@Param("hotelId") Long hotelId,
                                     @Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate);

    @Override
    @EntityGraph(attributePaths = "hotel")
    org.springframework.data.domain.Page<Room> findAll(
            org.springframework.data.jpa.domain.Specification<Room> spec,
            org.springframework.data.domain.Pageable pageable);
}
