package com.example.hotel.config;

import com.example.common.DemoStay;
import com.example.hotel.domain.Hotel;
import com.example.hotel.domain.Room;
import com.example.hotel.domain.RoomHold;
import com.example.hotel.enums.HoldStatus;
import com.example.hotel.repository.HotelRepository;
import com.example.hotel.repository.RoomHoldRepository;
import com.example.hotel.repository.RoomRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder {

    @Bean
    CommandLineRunner seedHotels(HotelRepository hotels, RoomRepository rooms, RoomHoldRepository holds) {
        return args -> {
            if (hotels.count() > 0) {
                return;
            }
            Hotel plaza = hotel(hotels, "Grand Plaza", "Tverskaya 1, Moscow", "City hotel next to the centre", 5);
            Hotel sea = hotel(hotels, "Sea Breeze", "Naberezhnaya 10, Sochi", "Quiet rooms by the water", 4);
            Room grand101 = room(rooms, plaza, DemoStay.ROOM_NUMBER, true, 5, 2, 1);
            room(rooms, plaza, "102", true, 1, 2, 1);
            room(rooms, plaza, "103", true, 0, 3, 2);
            room(rooms, plaza, "104", false, 2, 2, 2);
            room(rooms, sea, "201", true, 3, 2, 2);
            room(rooms, sea, "202", true, 0, 4, 2);

            RoomHold hold = new RoomHold();
            hold.setRoom(grand101);
            hold.setBookingId(DemoStay.BOOKING_ID);
            hold.setRequestId(DemoStay.REQUEST_ID);
            hold.setStartDate(DemoStay.start());
            hold.setEndDate(DemoStay.end());
            hold.setStatus(HoldStatus.ACTIVE);
            hold.setCreatedAt(Instant.now());
            holds.save(hold);
            log.info("Seeded hotels. Demo room {} id={} is held for booking {} on {}..{}",
                    grand101.getNumber(), grand101.getId(), DemoStay.BOOKING_ID, DemoStay.start(), DemoStay.end());
        };
    }

    private Hotel hotel(HotelRepository hotels, String name, String address, String description, int stars) {
        Hotel hotel = new Hotel();
        hotel.setName(name);
        hotel.setAddress(address);
        hotel.setDescription(description);
        hotel.setStars(stars);
        hotel.setCreatedAt(Instant.now());
        return hotels.save(hotel);
    }

    private Room room(RoomRepository rooms, Hotel hotel, String number, boolean available, int timesBooked, int capacity, int floor) {
        Room room = new Room();
        room.setHotel(hotel);
        room.setNumber(number);
        room.setAvailable(available);
        room.setTimesBooked(timesBooked);
        room.setCapacity(capacity);
        room.setFloor(floor);
        room.setCreatedAt(Instant.now());
        return rooms.save(room);
    }
}
