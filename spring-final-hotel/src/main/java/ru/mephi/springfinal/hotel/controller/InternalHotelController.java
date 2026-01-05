package ru.mephi.springfinal.hotel.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.mephi.springfinal.hotel.service.HotelService;

@RestController
@RequestMapping("/internal/hotels")
public class InternalHotelController {
    private final HotelService hotelService;

    public InternalHotelController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @PostMapping("/{hotelId}/rooms/{roomId}/confirm-availability")
    public ResponseEntity<Void> confirmAvailability(@PathVariable Long hotelId, @PathVariable Long roomId) {
        boolean ok = hotelService.confirmAvailability(hotelId, roomId);
        return ok ? ResponseEntity.ok().build() : ResponseEntity.status(409).build();
    }

    @PostMapping("/{hotelId}/rooms/{roomId}/release")
    public ResponseEntity<Void> release(@PathVariable Long hotelId, @PathVariable Long roomId) {
        hotelService.release(hotelId, roomId);
        return ResponseEntity.ok().build();
    }
}

