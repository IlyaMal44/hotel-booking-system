package com.example.hotel.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "rooms",
        uniqueConstraints = @UniqueConstraint(name = "uk_room_hotel_number", columnNames = {"hotel_id", "number"}),
        indexes = {
                @Index(name = "idx_room_times_booked", columnList = "times_booked,id"),
                @Index(name = "idx_room_hotel", columnList = "hotel_id")
        }
)
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(nullable = false, length = 20)
    private String number;

    @Column(nullable = false)
    private boolean available;

    @Column(name = "times_booked", nullable = false)
    private int timesBooked;

    @Column
    private Integer capacity;

    @Column
    private Integer floor;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
