package com.example.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "room_guards")
public class RoomGuard {

    @Id
    @Column(name = "room_id")
    private Long roomId;

    public RoomGuard(Long roomId) {
        this.roomId = roomId;
    }
}
