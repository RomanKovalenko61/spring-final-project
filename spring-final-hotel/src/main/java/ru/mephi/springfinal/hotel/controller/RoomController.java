package ru.mephi.springfinal.hotel.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.mephi.springfinal.hotel.dto.AvailabilityResponse;
import ru.mephi.springfinal.hotel.dto.ConfirmAvailabilityRequest;
import ru.mephi.springfinal.hotel.dto.RoomDto;
import ru.mephi.springfinal.hotel.service.RoomService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Tag(name = "Room Management", description = "Room management and availability operations")
@SecurityRequirement(name = "bearerAuth")
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    @Operation(summary = "Create a new room", description = "Admin only")
    public ResponseEntity<RoomDto> createRoom(@Valid @RequestBody RoomDto dto) {
        RoomDto created = roomService.createRoom(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "Get available rooms")
    public ResponseEntity<List<RoomDto>> getAvailableRooms(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<RoomDto> rooms = roomService.getAvailableRooms(startDate, endDate);
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/recommend")
    @Operation(summary = "Get recommended rooms (sorted by times_booked)")
    public ResponseEntity<List<RoomDto>> getRecommendedRooms(
            @RequestParam(required = false) Long hotelId,
            @RequestParam(required = false) String roomType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<RoomDto> rooms = roomService.getRecommendedRooms(hotelId, roomType, startDate, endDate);
        return ResponseEntity.ok(rooms);
    }

    @PostMapping("/{id}/confirm-availability")
    @Operation(summary = "Confirm room availability (internal)",
               description = "Used by Booking Service to reserve a room")
    public ResponseEntity<AvailabilityResponse> confirmAvailability(
            @PathVariable Long id,
            @Valid @RequestBody ConfirmAvailabilityRequest request) {
        AvailabilityResponse response = roomService.confirmAvailability(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/release")
    @Operation(summary = "Release room reservation (internal)",
               description = "Compensation action to release a temporary reservation")
    public ResponseEntity<Void> releaseReservation(
            @PathVariable Long id,
            @RequestParam String requestId) {
        roomService.releaseReservation(id, requestId);
        return ResponseEntity.noContent().build();
    }
}

