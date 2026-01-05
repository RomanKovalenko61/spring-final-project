package ru.mephi.springfinal.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mephi.springfinal.booking.model.Booking;

public interface BookingRepository extends JpaRepository<Booking, Long> {
}

