package com.example.booking;

import com.example.booking.client.HotelPort;
import com.example.booking.domain.Booking;
import com.example.booking.domain.User;
import com.example.booking.dto.ConfirmCommand;
import com.example.booking.dto.ConfirmView;
import com.example.booking.dto.CreateBookingRequest;
import com.example.booking.dto.RoomView;
import com.example.booking.enums.BookingStatus;
import com.example.booking.exception.ConflictException;
import com.example.booking.exception.HotelTransientException;
import com.example.booking.exception.RoomConflictException;
import com.example.booking.repository.BookingRepository;
import com.example.booking.repository.RoomGuardRepository;
import com.example.booking.repository.UserRepository;
import com.example.booking.service.BookingSagaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class BookingFlowTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoomGuardRepository roomGuardRepository;
    @Autowired
    private BookingSagaService bookingSagaService;
    @MockitoBean
    private HotelPort hotelPort;

    private LocalDate start;
    private LocalDate end;

    @BeforeEach
    void clean() {
        bookingRepository.deleteAll();
        roomGuardRepository.deleteAll();
        userRepository.deleteAll();
        start = LocalDate.now().plusDays(10);
        end = start.plusDays(3);
        doAnswer(invocation -> {
            ConfirmCommand command = invocation.getArgument(0);
            return new ConfirmView(command.roomId(), command.requestId().toString(), command.bookingId(), 1, true);
        }).when(hotelPort).confirm(any());
    }

    @Test
    @DisplayName("Регистрация возвращает JWT на час, неверный пароль дает 401")
    void registerAndLoginIssueJwt() throws Exception {
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"secret1"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expiresInSeconds").value(3600))
                .andExpect(jsonPath("$.user.role").value("USER"));

        mockMvc.perform(post("/api/user/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"wrong12"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Отель подтвердил номер - бронь становится CONFIRMED, release не зовем")
    void createBookingConfirmsWhenHotelAccepts() throws Exception {
        String token = register("alice");

        mockMvc.perform(post("/api/booking")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(UUID.randomUUID(), 4L, false)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.roomId").value(4));

        verify(hotelPort, times(1)).confirm(any());
        verify(hotelPort, never()).release(any());
    }

    @Test
    @DisplayName("Повтор того же requestId возвращает ту же бронь и не дергает отель второй раз")
    void sameRequestIdIsIdempotent() throws Exception {
        String token = register("alice");
        UUID requestId = UUID.randomUUID();

        String created = mockMvc.perform(post("/api/booking")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(requestId, 4L, false)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(post("/api/booking")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(requestId, 4L, false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(equalTo(com.jayway.jsonpath.JsonPath.read(created, "$.id"))));

        verify(hotelPort, times(1)).confirm(any());
    }

    @Test
    @DisplayName("Отель недоступен - бронь уходит в CANCELLED и вызывается release")
    void remoteFailureCancelsAndReleases(CapturedOutput output) throws Exception {
        String token = register("alice");
        doThrow(new HotelTransientException("timeout")).when(hotelPort).confirm(any());

        mockMvc.perform(post("/api/booking")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(UUID.randomUUID(), 4L, false)))
                .andExpect(status().isServiceUnavailable());

        assertThat(bookingRepository.findAll()).singleElement()
                .extracting(Booking::getStatus).isEqualTo(BookingStatus.CANCELLED);
        verify(hotelPort).release(any());
        assertThat(output).contains("transition=PENDING->CANCELLED").contains("compensation=release result=OK");
    }

    @Test
    @DisplayName("Отель ответил 409 - бронь отменяем, release не нужен, слот он не занимал")
    void conflictDoesNotReleaseTheRoom() throws Exception {
        String token = register("alice");
        doThrow(new RoomConflictException("taken")).when(hotelPort).confirm(any());

        mockMvc.perform(post("/api/booking")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(UUID.randomUUID(), 4L, false)))
                .andExpect(status().isConflict());

        verify(hotelPort, never()).release(any());
        assertThat(bookingRepository.findAll()).singleElement()
                .extracting(Booking::getStatus).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    @DisplayName("Пересечение дат с уже подтвержденной бронью отсекаем локально, до вызова отеля")
    void overlappingBookingIsRejectedBeforeHotelCall() throws Exception {
        String token = register("alice");
        User alice = userRepository.findByUsername("alice").orElseThrow();
        Booking existing = new Booking();
        existing.setUser(alice);
        existing.setRoomId(9L);
        existing.setStartDate(start);
        existing.setEndDate(end);
        existing.setStatus(BookingStatus.CONFIRMED);
        existing.setRequestId(UUID.randomUUID().toString());
        existing.setCreatedAt(Instant.now());
        existing.setUpdatedAt(existing.getCreatedAt());
        bookingRepository.save(existing);

        mockMvc.perform(post("/api/booking")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(UUID.randomUUID(), 9L, false)))
                .andExpect(status().isConflict());

        verify(hotelPort, never()).confirm(any());
    }

    @Test
    @DisplayName("Автоподбор идет от меньшего timesBooked и пропускает номер, который отель не отдал")
    void autoSelectSkipsBusyRoomAndKeepsTimesBookedOrder() throws Exception {
        String token = register("alice");
        doReturn(List.of(
                new RoomView(5L, 1L, "Plaza", "101", true, 0, 2, 1),
                new RoomView(8L, 1L, "Plaza", "102", true, 3, 2, 1)
        )).when(hotelPort).recommend(any(), any(), any());
        doAnswer(invocation -> {
            ConfirmCommand command = invocation.getArgument(0);
            if (command.roomId().equals(5L)) {
                throw new RoomConflictException("taken");
            }
            return new ConfirmView(command.roomId(), command.requestId().toString(), command.bookingId(), 4, true);
        }).when(hotelPort).confirm(any());

        mockMvc.perform(post("/api/booking")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(UUID.randomUUID(), null, true)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomId").value(8))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("Чужую бронь пользователь не читает, свою читает")
    void userSeesOnlyOwnBooking() throws Exception {
        String alice = register("alice");
        String bob = register("bob");
        String created = mockMvc.perform(post("/api/booking")
                        .header("Authorization", "Bearer " + alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(UUID.randomUUID(), 4L, false)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int id = com.jayway.jsonpath.JsonPath.read(created, "$.id");

        mockMvc.perform(get("/api/booking/" + id).header("Authorization", "Bearer " + bob))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/booking/" + id).header("Authorization", "Bearer " + alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    @DisplayName("История броней режется по страницам и считает общее число")
    void historyIsPaginated() throws Exception {
        String token = register("alice");
        for (int i = 0; i < 3; i++) {
            LocalDate from = start.plusDays(i * 4L);
            mockMvc.perform(post("/api/booking")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bookingJson(UUID.randomUUID(), 20L + i, false, from, from.plusDays(2))))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/bookings")
                        .header("Authorization", "Bearer " + token)
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    @DisplayName("Дата выезда раньше заезда дает 400, запрос без токена дает 401")
    void invalidDatesAndMissingTokenAreRejected() throws Exception {
        String token = register("alice");
        mockMvc.perform(post("/api/booking")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(UUID.randomUUID(), 4L, false, end, start)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/booking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(UUID.randomUUID(), 4L, false)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Обычный пользователь не создает учетки, админ создает")
    void userCannotAdministerAccounts() throws Exception {
        String token = register("alice");
        mockMvc.perform(post("/api/user")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"extra","password":"secret1","role":"USER"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/user")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"extra","password":"secret1","role":"USER"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Отмена подтвержденной брони ставит CANCELLED и зовет release")
    void cancelConfirmedBookingReleasesTheHold() throws Exception {
        String token = register("alice");
        String created = mockMvc.perform(post("/api/booking")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(UUID.randomUUID(), 4L, false)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int id = com.jayway.jsonpath.JsonPath.read(created, "$.id");

        mockMvc.perform(delete("/api/booking/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        verify(hotelPort).release(any());
    }

    @Test
    @DisplayName("Две параллельные брони одного номера на одни даты - проходит только одна")
    void parallelBookingsOfOneRoomAllowASingleConfirmation() throws Exception {
        register("alice");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();
        try {
            Future<?> first = pool.submit(() -> book(latch, success, conflict));
            Future<?> second = pool.submit(() -> book(latch, success, conflict));
            latch.countDown();
            first.get();
            second.get();
        } finally {
            pool.shutdownNow();
        }
        assertThat(success.get()).isEqualTo(1);
        assertThat(conflict.get()).isEqualTo(1);
        assertThat(bookingRepository.findAll())
                .filteredOn(booking -> booking.getStatus() == BookingStatus.CONFIRMED)
                .hasSize(1);
    }

    private void book(CountDownLatch latch, AtomicInteger success, AtomicInteger conflict) {
        try {
            latch.await();
            bookingSagaService.create(new CreateBookingRequest(UUID.randomUUID(), start, end, 77L, null, false), "alice");
            success.incrementAndGet();
        } catch (ConflictException exception) {
            conflict.incrementAndGet();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private String register(String username) throws Exception {
        String body = mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"secret1\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(body, "$.token");
    }

    private String bookingJson(UUID requestId, Long roomId, boolean autoSelect) {
        return bookingJson(requestId, roomId, autoSelect, start, end);
    }

    private String bookingJson(UUID requestId, Long roomId, boolean autoSelect, LocalDate from, LocalDate to) {
        return """
                {"requestId":"%s","startDate":"%s","endDate":"%s","roomId":%s,"autoSelect":%s}
                """.formatted(requestId, from, to, roomId == null ? "null" : roomId, autoSelect);
    }
}
