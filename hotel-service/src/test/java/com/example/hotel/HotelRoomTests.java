package com.example.hotel;

import com.example.hotel.domain.Hotel;
import com.example.hotel.domain.Room;
import com.example.hotel.dto.ConfirmAvailabilityRequest;
import com.example.hotel.exception.ConflictException;
import com.example.hotel.repository.HotelRepository;
import com.example.hotel.repository.RoomHoldRepository;
import com.example.hotel.repository.RoomRepository;
import com.example.hotel.service.RoomAvailabilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HotelRoomTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private HotelRepository hotelRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private RoomHoldRepository roomHoldRepository;
    @Autowired
    private RoomAvailabilityService roomAvailabilityService;

    @BeforeEach
    void clean() {
        roomHoldRepository.deleteAll();
        roomRepository.deleteAll();
        hotelRepository.deleteAll();
    }

    @Test
    @DisplayName("Список отелей без токена закрыт")
    void anonymousListIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/hotels")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Обычный пользователь не создает отель")
    void userCannotCreateHotel() throws Exception {
        mockMvc.perform(post("/api/hotels")
                        .with(userJwt())
                        .contentType("application/json")
                        .content("""
                                {"name":"A","address":"B"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Рекомендация идет от меньшего timesBooked и не показывает номер на ремонте")
    void recommendSortsByTimesBookedThenId() throws Exception {
        Hotel hotel = saveHotel("Plaza");
        saveRoom(hotel, "301", 5);
        Room least = saveRoom(hotel, "302", 0);
        Room middle = saveRoom(hotel, "303", 2);
        saveRoom(hotel, "304", 9, false);

        mockMvc.perform(get("/api/rooms/recommend").with(userJwt()).param("hotelId", hotel.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(least.getId()))
                .andExpect(jsonPath("$[1].id").value(middle.getId()))
                .andExpect(jsonPath("$[2].timesBooked").value(5))
                .andExpect(jsonPath("$[3]").doesNotExist());
    }

    @Test
    @DisplayName("Статистику видит только админ, простой считается по нулевому timesBooked")
    void statisticsAreAdminOnlyAndCountIdleRooms() throws Exception {
        Hotel hotel = saveHotel("Plaza");
        saveRoom(hotel, "1", 0);
        saveRoom(hotel, "2", 4);

        mockMvc.perform(get("/api/rooms/statistics").with(userJwt()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/rooms/statistics").with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRooms").value(2))
                .andExpect(jsonPath("$.idleRooms").value(1))
                .andExpect(jsonPath("$.maxTimesBooked").value(4));
    }

    @Test
    @DisplayName("Повтор confirm не увеличивает счетчик, пересечение дат дает конфликт")
    void confirmIsIdempotentAndRejectsOverlap() {
        Room room = saveRoom(saveHotel("Plaza"), "10", 0);
        LocalDate start = LocalDate.now().plusDays(5);
        LocalDate end = start.plusDays(2);
        ConfirmAvailabilityRequest first = new ConfirmAvailabilityRequest(UUID.randomUUID(), 10L, start, end);

        var locked = roomAvailabilityService.confirm(room.getId(), first);
        var replay = roomAvailabilityService.confirm(room.getId(), first);

        assertThat(locked.locked()).isTrue();
        assertThat(replay.timesBooked()).isEqualTo(1);
        assertThat(roomRepository.findById(room.getId()).orElseThrow().getTimesBooked()).isEqualTo(1);

        ConfirmAvailabilityRequest overlap = new ConfirmAvailabilityRequest(UUID.randomUUID(), 11L, start.plusDays(1), end.plusDays(1));
        org.junit.jupiter.api.Assertions.assertThrows(ConflictException.class,
                () -> roomAvailabilityService.confirm(room.getId(), overlap));
        assertThat(roomRepository.findById(room.getId()).orElseThrow().getTimesBooked()).isEqualTo(1);
    }

    @Test
    @DisplayName("Первый release уменьшает счетчик, второй уже ничего не меняет")
    void releaseIsIdempotent() {
        Room room = saveRoom(saveHotel("Plaza"), "11", 0);
        LocalDate start = LocalDate.now().plusDays(8);
        UUID requestId = UUID.randomUUID();
        roomAvailabilityService.confirm(room.getId(), new ConfirmAvailabilityRequest(requestId, 3L, start, start.plusDays(1)));

        var released = roomAvailabilityService.release(room.getId(), new com.example.hotel.dto.ReleaseRequest(requestId, 3L));
        var again = roomAvailabilityService.release(room.getId(), new com.example.hotel.dto.ReleaseRequest(requestId, 3L));

        assertThat(released.released()).isTrue();
        assertThat(again.released()).isFalse();
        assertThat(roomRepository.findById(room.getId()).orElseThrow().getTimesBooked()).isZero();
    }

    @Test
    @DisplayName("Два одновременных confirm на одни даты - блокировка только у одного")
    void parallelConfirmsAllowOnlyOneLock() throws Exception {
        Room room = saveRoom(saveHotel("Plaza"), "12", 0);
        LocalDate start = LocalDate.now().plusDays(12);
        LocalDate end = start.plusDays(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();
        try {
            Future<?> first = pool.submit(() -> confirmAfter(startLatch, room.getId(), start, end, success, conflict));
            Future<?> second = pool.submit(() -> confirmAfter(startLatch, room.getId(), start, end, success, conflict));
            startLatch.countDown();
            first.get();
            second.get();
        } finally {
            pool.shutdownNow();
        }
        assertThat(success.get()).isEqualTo(1);
        assertThat(conflict.get()).isEqualTo(1);
        assertThat(roomRepository.findById(room.getId()).orElseThrow().getTimesBooked()).isEqualTo(1);
    }

    private void confirmAfter(CountDownLatch latch, Long roomId, LocalDate start, LocalDate end,
                              AtomicInteger success, AtomicInteger conflict) {
        try {
            latch.await();
            roomAvailabilityService.confirm(roomId, new ConfirmAvailabilityRequest(UUID.randomUUID(), UUID.randomUUID().getMostSignificantBits(), start, end));
            success.incrementAndGet();
        } catch (ConflictException exception) {
            conflict.incrementAndGet();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private Hotel saveHotel(String name) {
        Hotel hotel = new Hotel();
        hotel.setName(name);
        hotel.setAddress("Address");
        hotel.setStars(4);
        hotel.setCreatedAt(Instant.now());
        return hotelRepository.save(hotel);
    }

    private Room saveRoom(Hotel hotel, String number, int timesBooked) {
        return saveRoom(hotel, number, timesBooked, true);
    }

    private Room saveRoom(Hotel hotel, String number, int timesBooked, boolean available) {
        Room room = new Room();
        room.setHotel(hotel);
        room.setNumber(number);
        room.setAvailable(available);
        room.setTimesBooked(timesBooked);
        room.setCreatedAt(Instant.now());
        return roomRepository.save(room);
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor userJwt() {
        return jwt().jwt(token -> token.subject("user")).authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor adminJwt() {
        return jwt().jwt(token -> token.subject("admin")).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
