package ru.mephi.springfinal.hotel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mephi.springfinal.hotel.model.Room;

import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {
    Optional<Room> findByHotelIdAndId(Long hotelId, Long roomId);
}

