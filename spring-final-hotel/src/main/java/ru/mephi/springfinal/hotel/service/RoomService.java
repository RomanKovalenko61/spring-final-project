package ru.mephi.springfinal.hotel.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mephi.springfinal.hotel.dto.AvailabilityResponse;
import ru.mephi.springfinal.hotel.dto.ConfirmAvailabilityRequest;
import ru.mephi.springfinal.hotel.dto.RoomDto;
import ru.mephi.springfinal.hotel.entity.Hotel;
import ru.mephi.springfinal.hotel.entity.Room;
import ru.mephi.springfinal.hotel.entity.RoomReservation;
import ru.mephi.springfinal.hotel.mapper.RoomMapper;
import ru.mephi.springfinal.hotel.repository.HotelRepository;
import ru.mephi.springfinal.hotel.repository.RoomRepository;
import ru.mephi.springfinal.hotel.repository.RoomReservationRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final RoomReservationRepository reservationRepository;
    private final RoomMapper roomMapper;

    private static final long RESERVATION_TIMEOUT_MINUTES = 5;

    @Transactional
    public RoomDto createRoom(RoomDto dto) {
        log.info("Creating room: {} for hotel: {}", dto.getRoomNumber(), dto.getHotelId());

        Hotel hotel = hotelRepository.findById(dto.getHotelId())
                .orElseThrow(() -> new RuntimeException("Hotel not found with id: " + dto.getHotelId()));

        Room room = roomMapper.toEntity(dto);
        room.setHotel(hotel);
        room.setTimesBooked(0);
        room.setAvailable(true);

        Room saved = roomRepository.save(room);
        return roomMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<RoomDto> getAvailableRooms(LocalDate startDate, LocalDate endDate) {
        log.info("Fetching available rooms for period: {} - {}", startDate, endDate);
        List<Room> rooms = roomRepository.findAvailableRooms(startDate, endDate);
        return roomMapper.toDtoList(rooms);
    }

    @Transactional(readOnly = true)
    public List<RoomDto> getRecommendedRooms(Long hotelId, String roomType,
                                             LocalDate startDate, LocalDate endDate) {
        log.info("Fetching recommended rooms for hotel: {}, type: {}, period: {} - {}",
                 hotelId, roomType, startDate, endDate);

        if (roomType != null) {
            Room.RoomType type = Room.RoomType.valueOf(roomType.toUpperCase());
            List<Room> rooms = roomRepository.findAvailableRoomsByTypeRecommended(type, startDate, endDate);
            return roomMapper.toDtoList(rooms);
        } else if (hotelId != null) {
            List<Room> rooms = roomRepository.findAvailableRoomsByHotelRecommended(hotelId, startDate, endDate);
            return roomMapper.toDtoList(rooms);
        } else {
            return getAvailableRooms(startDate, endDate);
        }
    }

    @Transactional
    public AvailabilityResponse confirmAvailability(Long roomId, ConfirmAvailabilityRequest request) {
        log.info("Confirming availability for room: {}, requestId: {}, bookingId: {}",
                 roomId, request.getRequestId(), request.getBookingId());

        // Идемпотентность: проверка, обрабатывался ли этот requestId ранее
        Optional<RoomReservation> existing = reservationRepository.findByRequestId(request.getRequestId());
        if (existing.isPresent()) {
            RoomReservation reservation = existing.get();
            log.info("Request already processed: {}, status: {}", request.getRequestId(), reservation.getStatus());

            if (reservation.getStatus() == RoomReservation.ReservationStatus.CONFIRMED ||
                reservation.getStatus() == RoomReservation.ReservationStatus.PENDING) {
                return AvailabilityResponse.success(roomId);
            } else {
                return AvailabilityResponse.failure("Reservation was already released or expired");
            }
        }

        // Проверка существования комнаты
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found with id: " + roomId));

        if (!room.getAvailable()) {
            return AvailabilityResponse.failure("Room is not available (maintenance)");
        }

        // Создание временной резервации
        RoomReservation reservation = new RoomReservation();
        reservation.setRequestId(request.getRequestId());
        reservation.setBookingId(request.getBookingId());
        reservation.setRoomId(roomId);
        reservation.setStartDate(request.getStartDate());
        reservation.setEndDate(request.getEndDate());
        reservation.setStatus(RoomReservation.ReservationStatus.CONFIRMED);
        reservation.setCreatedAt(LocalDateTime.now());
        reservation.setExpiresAt(LocalDateTime.now().plusMinutes(RESERVATION_TIMEOUT_MINUTES));

        try {
            reservationRepository.save(reservation);

            // Увеличение счетчика бронирований
            room.setTimesBooked(room.getTimesBooked() + 1);
            roomRepository.save(room);

            log.info("Room {} successfully reserved for booking {}", roomId, request.getBookingId());
            return AvailabilityResponse.success(roomId);
        } catch (Exception e) {
            log.error("Failed to reserve room {}: {}", roomId, e.getMessage());
            return AvailabilityResponse.failure("Room is already reserved for this period");
        }
    }

    @Transactional
    public void releaseReservation(Long roomId, String requestId) {
        log.info("Releasing reservation for room: {}, requestId: {}", roomId, requestId);

        Optional<RoomReservation> reservationOpt = reservationRepository.findByRequestId(requestId);
        if (reservationOpt.isEmpty()) {
            log.warn("Reservation not found for requestId: {}", requestId);
            return;
        }

        RoomReservation reservation = reservationOpt.get();
        if (reservation.getStatus() == RoomReservation.ReservationStatus.RELEASED ||
            reservation.getStatus() == RoomReservation.ReservationStatus.EXPIRED) {
            log.info("Reservation already released: {}", requestId);
            return;
        }

        reservation.setStatus(RoomReservation.ReservationStatus.RELEASED);
        reservationRepository.save(reservation);

        // Уменьшение счетчика бронирований
        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isPresent()) {
            Room room = roomOpt.get();
            if (room.getTimesBooked() > 0) {
                room.setTimesBooked(room.getTimesBooked() - 1);
                roomRepository.save(room);
            }
        }

        log.info("Reservation released for room: {}", roomId);
    }

    @Transactional
    public void cleanupExpiredReservations() {
        log.info("Cleaning up expired reservations");
        LocalDateTime now = LocalDateTime.now();
        List<RoomReservation> expired = reservationRepository.findExpiredReservations(now);

        for (RoomReservation reservation : expired) {
            reservation.setStatus(RoomReservation.ReservationStatus.EXPIRED);

            // Уменьшение счетчика бронирований
            Optional<Room> roomOpt = roomRepository.findById(reservation.getRoomId());
            if (roomOpt.isPresent()) {
                Room room = roomOpt.get();
                if (room.getTimesBooked() > 0) {
                    room.setTimesBooked(room.getTimesBooked() - 1);
                    roomRepository.save(room);
                }
            }
        }

        reservationRepository.saveAll(expired);
        log.info("Cleaned up {} expired reservations", expired.size());
    }
}

