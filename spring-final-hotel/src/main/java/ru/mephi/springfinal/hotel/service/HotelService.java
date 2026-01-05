package ru.mephi.springfinal.hotel.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mephi.springfinal.hotel.model.Hotel;
import ru.mephi.springfinal.hotel.model.Room;
import ru.mephi.springfinal.hotel.repository.HotelRepository;
import ru.mephi.springfinal.hotel.repository.RoomRepository;

import java.util.List;
import java.util.Optional;

@Service
public class HotelService {
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;

    public HotelService(HotelRepository hotelRepository, RoomRepository roomRepository) {
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
    }

    public Hotel saveHotel(Hotel hotel) {
        return hotelRepository.save(hotel);
    }

    public Optional<Hotel> findById(Long id) {
        return hotelRepository.findById(id);
    }

    public List<Hotel> findAll() {
        return hotelRepository.findAll();
    }

    @Transactional
    public Room addRoom(Long hotelId, Room room) {
        Hotel hotel = hotelRepository.findById(hotelId).orElseThrow();
        room.setHotel(hotel);
        return roomRepository.save(room);
    }

    public Optional<Room> findRoom(Long hotelId, Long roomId) {
        return roomRepository.findByHotelIdAndId(hotelId, roomId);
    }

    @Transactional
    public boolean confirmAvailability(Long hotelId, Long roomId) {
        Room room = findRoom(hotelId, roomId).orElseThrow();
        if (!room.isAvailable()) return false;
        room.setAvailable(false);
        roomRepository.save(room);
        return true;
    }

    @Transactional
    public void release(Long hotelId, Long roomId) {
        Room room = findRoom(hotelId, roomId).orElseThrow();
        room.setAvailable(true);
        roomRepository.save(room);
    }
}

