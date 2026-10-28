package com.example.booking.controller;

import com.example.booking.dto.BookingCreateResult;
import com.example.booking.dto.BookingResponse;
import com.example.booking.dto.CreateBookingRequest;
import com.example.booking.dto.RoomView;
import com.example.booking.service.BookingQueryService;
import com.example.booking.service.BookingSagaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@Tag(name = "Бронирования")
@SecurityRequirement(name = "bearerAuth")
public class BookingController {

    private final BookingSagaService bookingSagaService;
    private final BookingQueryService bookingQueryService;

    public BookingController(BookingSagaService bookingSagaService, BookingQueryService bookingQueryService) {
        this.bookingSagaService = bookingSagaService;
        this.bookingQueryService = bookingQueryService;
    }

    @PostMapping({"/api/booking", "/booking"})
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Operation(summary = "Создать бронирование, при autoSelect=true поле roomId игнорируется и номер берётся по возрастанию timesBooked")
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody CreateBookingRequest request, @AuthenticationPrincipal Jwt jwt) {
        BookingCreateResult result = bookingSagaService.create(request, jwt.getSubject());
        if (result.replay()) {
            return ResponseEntity.ok(result.booking());
        }
        return ResponseEntity.created(URI.create("/api/booking/" + result.booking().id())).body(result.booking());
    }

    @GetMapping({"/api/bookings", "/bookings"})
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Operation(summary = "История бронирований текущего пользователя")
    public Page<BookingResponse> history(@AuthenticationPrincipal Jwt jwt,
                                         @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return bookingQueryService.history(jwt.getSubject(), pageable);
    }

    @GetMapping({"/api/bookings/recommendations", "/bookings/recommendations"})
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Operation(summary = "Свободные номера на выбранные даты, сначала с меньшим timesBooked, при равенстве по id")
    public List<RoomView> recommendations(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                          @RequestParam(required = false) Long hotelId) {
        return bookingQueryService.recommend(startDate, endDate, hotelId);
    }

    @GetMapping({"/api/booking/{id}", "/booking/{id}"})
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    /** Бронь по id, чужую обычному пользователю не отдаем */
    public BookingResponse get(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        return bookingQueryService.get(id, jwt.getSubject(), isAdmin(authentication));
    }

    @DeleteMapping({"/api/booking/{id}", "/booking/{id}"})
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Operation(summary = "Отменить бронирование и снять блокировку номера")
    public BookingResponse cancel(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        return bookingSagaService.cancel(id, jwt.getSubject(), isAdmin(authentication));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
