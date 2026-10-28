package com.example.hotel.controller;

import com.example.hotel.dto.ConfirmAvailabilityRequest;
import com.example.hotel.dto.ConfirmAvailabilityResponse;
import com.example.hotel.dto.OccupancyStatisticsResponse;
import com.example.hotel.dto.ReleaseRequest;
import com.example.hotel.dto.ReleaseResponse;
import com.example.hotel.dto.RoomPatchRequest;
import com.example.hotel.dto.RoomRequest;
import com.example.hotel.dto.RoomResponse;
import com.example.hotel.service.OccupancyStatisticsService;
import com.example.hotel.service.RoomAvailabilityService;
import com.example.hotel.service.RoomCommandService;
import com.example.hotel.service.RoomQueryService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/rooms")
@Tag(name = "Номера")
@SecurityRequirement(name = "bearerAuth")
public class RoomController {

    private final RoomQueryService roomQueryService;
    private final RoomCommandService roomCommandService;
    private final RoomAvailabilityService roomAvailabilityService;
    private final OccupancyStatisticsService occupancyStatisticsService;

    public RoomController(RoomQueryService roomQueryService,
                          RoomCommandService roomCommandService,
                          RoomAvailabilityService roomAvailabilityService,
                          OccupancyStatisticsService occupancyStatisticsService) {
        this.roomQueryService = roomQueryService;
        this.roomCommandService = roomCommandService;
        this.roomAvailabilityService = roomAvailabilityService;
        this.occupancyStatisticsService = occupancyStatisticsService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Добавить номер в отель")
    public ResponseEntity<RoomResponse> create(@Valid @RequestBody RoomRequest request) {
        RoomResponse response = roomCommandService.create(request);
        return ResponseEntity.created(URI.create("/api/rooms/" + response.id())).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Operation(summary = "Список рабочих номеров, available=false показывает снятые с эксплуатации")
    public Page<RoomResponse> list(@RequestParam(required = false) Long hotelId,
                                   @RequestParam(required = false) Boolean available,
                                   @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return roomQueryService.list(hotelId, available, pageable);
    }

    @GetMapping("/recommend")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Operation(summary = "Свободные номера по возрастанию timesBooked, при равенстве по id")
    public java.util.List<RoomResponse> recommend(@RequestParam(required = false) Long hotelId,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return roomQueryService.recommend(hotelId, startDate, endDate);
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Статистика загруженности номеров")
    public OccupancyStatisticsResponse statistics(@RequestParam(required = false) Long hotelId) {
        return occupancyStatisticsService.statistics(hotelId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    /** Один номер по id */
    public RoomResponse get(@PathVariable Long id) {
        return roomQueryService.get(id);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    /** Частичное обновление номера */
    public RoomResponse update(@PathVariable Long id, @Valid @RequestBody RoomPatchRequest request) {
        return roomCommandService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    /** Удаление номера без активной блокировки */
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roomCommandService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/confirm-availability")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Operation(summary = "Внутренний вызов: временно заблокировать номер на даты саги, через Gateway не открыт")
    public ConfirmAvailabilityResponse confirm(@PathVariable Long id, @Valid @RequestBody ConfirmAvailabilityRequest request) {
        return roomAvailabilityService.confirm(id, request);
    }

    @PostMapping("/{id}/release")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Operation(summary = "Компенсация: снять блокировку номера, через Gateway не открыт")
    public ReleaseResponse release(@PathVariable Long id, @Valid @RequestBody ReleaseRequest request) {
        return roomAvailabilityService.release(id, request);
    }
}
