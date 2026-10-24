package com.example.hotel.controller;

import com.example.hotel.dto.HotelPatchRequest;
import com.example.hotel.dto.HotelRequest;
import com.example.hotel.dto.HotelResponse;
import com.example.hotel.service.HotelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

@RestController
@RequestMapping("/api/hotels")
@Tag(name = "Hotels")
@SecurityRequirement(name = "bearerAuth")
public class HotelController {

    private final HotelService hotelService;

    public HotelController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Добавить отель")
    public ResponseEntity<HotelResponse> create(@Valid @RequestBody HotelRequest request) {
        HotelResponse response = hotelService.create(request);
        return ResponseEntity.created(URI.create("/api/hotels/" + response.id())).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Operation(summary = "Список отелей, можно отфильтровать по названию, адресу и числу звёзд")
    public Page<HotelResponse> list(@RequestParam(required = false) String name,
                                    @RequestParam(required = false) String address,
                                    @RequestParam(required = false) Integer minStars,
                                    @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return hotelService.list(name, address, minStars, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    /** Карточка одного отеля */
    public HotelResponse get(@PathVariable Long id) {
        return hotelService.get(id);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    /** Частичное обновление отеля */
    public HotelResponse update(@PathVariable Long id, @Valid @RequestBody HotelPatchRequest request) {
        return hotelService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    /** Удаление отеля без номеров */
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        hotelService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
