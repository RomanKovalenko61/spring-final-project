package ru.mephi.springfinal.hotel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mephi.springfinal.hotel.model.Hotel;

public interface HotelRepository extends JpaRepository<Hotel, Long> {
}

