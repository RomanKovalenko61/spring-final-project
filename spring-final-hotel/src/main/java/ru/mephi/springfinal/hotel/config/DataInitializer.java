package ru.mephi.springfinal.hotel.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.mephi.springfinal.hotel.entity.Hotel;
import ru.mephi.springfinal.hotel.entity.Room;
import ru.mephi.springfinal.hotel.repository.HotelRepository;
import ru.mephi.springfinal.hotel.repository.RoomRepository;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;

    @Override
    public void run(String... args) {
        if (hotelRepository.count() == 0) {
            log.info("Initializing sample hotels and rooms...");

            // Hotel 1
            Hotel hotel1 = new Hotel();
            hotel1.setName("Grand Hotel");
            hotel1.setAddress("123 Main Street, Moscow");
            hotel1 = hotelRepository.save(hotel1);

            createRoom(hotel1, "101", Room.RoomType.SINGLE, "5000.00");
            createRoom(hotel1, "102", Room.RoomType.SINGLE, "5000.00");
            createRoom(hotel1, "201", Room.RoomType.DOUBLE, "8000.00");
            createRoom(hotel1, "202", Room.RoomType.DOUBLE, "8000.00");
            createRoom(hotel1, "301", Room.RoomType.SUITE, "15000.00");

            // Hotel 2
            Hotel hotel2 = new Hotel();
            hotel2.setName("Business Hotel");
            hotel2.setAddress("456 Business Ave, Moscow");
            hotel2 = hotelRepository.save(hotel2);

            createRoom(hotel2, "101", Room.RoomType.SINGLE, "4500.00");
            createRoom(hotel2, "102", Room.RoomType.DOUBLE, "7500.00");
            createRoom(hotel2, "103", Room.RoomType.DOUBLE, "7500.00");
            createRoom(hotel2, "201", Room.RoomType.SUITE, "12000.00");
            createRoom(hotel2, "202", Room.RoomType.DELUXE, "20000.00");

            log.info("Sample data initialized: 2 hotels with {} rooms", roomRepository.count());
        }
    }

    private void createRoom(Hotel hotel, String number, Room.RoomType type, String price) {
        Room room = new Room();
        room.setHotel(hotel);
        room.setRoomNumber(number);
        room.setType(type);
        room.setPricePerNight(new BigDecimal(price));
        room.setTimesBooked(0);
        room.setAvailable(true);
        roomRepository.save(room);
    }
}

