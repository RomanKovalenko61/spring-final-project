package ru.mephi.springfinal.booking.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mephi.springfinal.booking.client.HotelClient;
import ru.mephi.springfinal.booking.model.Booking;
import ru.mephi.springfinal.booking.model.BookingStatus;
import ru.mephi.springfinal.booking.repository.BookingRepository;

@Service
public class BookingService {
    private final BookingRepository bookingRepository;
    private final HotelClient hotelClient;

    public BookingService(BookingRepository bookingRepository, HotelClient hotelClient) {
        this.bookingRepository = bookingRepository;
        this.hotelClient = hotelClient;
    }

    @Transactional
    public Booking createBooking(Booking booking) {
        // сохранить как PENDING
        booking.setStatus(BookingStatus.PENDING);
        Booking saved = bookingRepository.save(booking);

        // Попытаться подтвердить availability через internal hotel API используя discovery
        boolean ok = hotelClient.confirmAvailability(booking.getHotelId(), booking.getRoomId());
        if (ok) {
            saved.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(saved);
        }

        return saved;
    }

    @Transactional
    public void cancelBooking(Long id) {
        Booking booking = bookingRepository.findById(id).orElseThrow();
        // если был подтвержден — отпустить комнату
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            hotelClient.release(booking.getHotelId(), booking.getRoomId());
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
        } else {
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
        }
    }
}
