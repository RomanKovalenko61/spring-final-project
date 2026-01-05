package ru.mephi.springfinal.hotel.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.mephi.springfinal.hotel.model.Hotel;
import ru.mephi.springfinal.hotel.model.Room;
import ru.mephi.springfinal.hotel.service.HotelService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/hotels")
public class HotelController {
    private final HotelService hotelService;

    public HotelController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @PostMapping
    public ResponseEntity<Hotel> create(@RequestBody Hotel hotel) {
        Hotel saved = hotelService.saveHotel(hotel);
        return ResponseEntity.created(URI.create("/api/hotels/" + saved.getId())).body(saved);
    }

    @GetMapping
    public List<Hotel> list() {
        return hotelService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Hotel> get(@PathVariable Long id) {
        return hotelService.findById(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/rooms")
    public ResponseEntity<Room> addRoom(@PathVariable Long id, @RequestBody Room room) {
        Room saved = hotelService.addRoom(id, room);
        return ResponseEntity.created(URI.create("/api/hotels/" + id + "/rooms/" + saved.getId())).body(saved);
    }
}

