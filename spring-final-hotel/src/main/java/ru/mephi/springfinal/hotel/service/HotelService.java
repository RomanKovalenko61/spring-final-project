package ru.mephi.springfinal.hotel.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mephi.springfinal.hotel.dto.HotelDto;
import ru.mephi.springfinal.hotel.entity.Hotel;
import ru.mephi.springfinal.hotel.mapper.HotelMapper;
import ru.mephi.springfinal.hotel.repository.HotelRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotelService {

    private final HotelRepository hotelRepository;
    private final HotelMapper hotelMapper;

    @Transactional
    public HotelDto createHotel(HotelDto dto) {
        log.info("Creating hotel: {}", dto.getName());
        Hotel hotel = hotelMapper.toEntity(dto);
        Hotel saved = hotelRepository.save(hotel);
        return hotelMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<HotelDto> getAllHotels() {
        log.info("Fetching all hotels");
        List<Hotel> hotels = hotelRepository.findAll();
        return hotelMapper.toDtoList(hotels);
    }

    @Transactional(readOnly = true)
    public HotelDto getHotelById(Long id) {
        log.info("Fetching hotel by id: {}", id);
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hotel not found with id: " + id));
        return hotelMapper.toDtoWithRooms(hotel);
    }

    @Transactional
    public HotelDto updateHotel(Long id, HotelDto dto) {
        log.info("Updating hotel with id: {}", id);
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hotel not found with id: " + id));
        hotelMapper.updateEntity(dto, hotel);
        Hotel updated = hotelRepository.save(hotel);
        return hotelMapper.toDto(updated);
    }

    @Transactional
    public void deleteHotel(Long id) {
        log.info("Deleting hotel with id: {}", id);
        hotelRepository.deleteById(id);
    }
}

