package ru.mephi.springfinal.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mephi.springfinal.booking.client.HotelServiceClient;
import ru.mephi.springfinal.booking.dto.BookingDto;
import ru.mephi.springfinal.booking.entity.Booking;
import ru.mephi.springfinal.booking.repository.BookingRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final HotelServiceClient hotelServiceClient;

    @Value("${booking.pending-timeout-minutes:5}")
    private int pendingTimeoutMinutes;

    @Transactional
    public BookingDto createBooking(BookingDto dto) {
        String requestId = dto.getRequestId() != null ? dto.getRequestId() : UUID.randomUUID().toString();
        log.info("Creating booking with requestId: {}", requestId);

        // Идемпотентность: проверка, существует ли бронирование с таким requestId
        Optional<Booking> existing = bookingRepository.findByRequestId(requestId);
        if (existing.isPresent()) {
            log.info("Booking already exists for requestId: {}", requestId);
            return toDto(existing.get());
        }

        // Создание бронирования в статусе PENDING
        Booking booking = new Booking();
        booking.setRequestId(requestId);
        booking.setUserId(dto.getUserId());
        booking.setHotelId(dto.getHotelId());
        booking.setStartDate(dto.getStartDate());
        booking.setEndDate(dto.getEndDate());
        booking.setStatus(Booking.BookingStatus.PENDING);
        booking.setExpiresAt(LocalDateTime.now().plusMinutes(pendingTimeoutMinutes));

        Booking savedBooking = bookingRepository.save(booking);
        log.info("Created PENDING booking: id={}, requestId={}", savedBooking.getId(), requestId);

        // Попытка подтверждения доступности номера
        try {
            boolean confirmed = false;

            if (dto.getAutoSelect() != null && dto.getAutoSelect()) {
                // Автоподбор комнаты
                confirmed = autoSelectAndConfirmRoom(savedBooking, dto);
            } else if (dto.getRoomId() != null) {
                // Ручной выбор комнаты
                confirmed = confirmSpecificRoom(savedBooking, dto.getRoomId());
            } else {
                throw new RuntimeException("Either roomId or autoSelect must be specified");
            }

            if (confirmed) {
                savedBooking.setStatus(Booking.BookingStatus.CONFIRMED);
                savedBooking = bookingRepository.save(savedBooking);
                log.info("Booking CONFIRMED: id={}, roomId={}", savedBooking.getId(), savedBooking.getRoomId());
            } else {
                compensateBooking(savedBooking, "No available rooms");
            }

        } catch (Exception e) {
            log.error("Error confirming booking: {}", e.getMessage(), e);
            compensateBooking(savedBooking, "Error: " + e.getMessage());
        }

        return toDto(savedBooking);
    }

    private boolean autoSelectAndConfirmRoom(Booking booking, BookingDto dto) {
        log.info("Auto-selecting room for booking: {}", booking.getId());

        // Получение рекомендованных комнат (отсортированных по times_booked)
        List<Map<String, Object>> rooms = hotelServiceClient.getRecommendedRooms(
                dto.getHotelId(),
                dto.getRoomType(),
                dto.getStartDate(),
                dto.getEndDate()
        );

        if (rooms.isEmpty()) {
            log.warn("No available rooms found for booking: {}", booking.getId());
            return false;
        }

        // Попытка подтверждения каждой комнаты по порядку
        for (Map<String, Object> room : rooms) {
            Long roomId = getLongValue(room.get("id"));
            log.info("Trying to confirm room: {} for booking: {}", roomId, booking.getId());

            Map<String, Object> response = hotelServiceClient.confirmAvailability(
                    roomId,
                    booking.getRequestId(),
                    booking.getId(),
                    booking.getStartDate(),
                    booking.getEndDate()
            );

            Boolean available = (Boolean) response.get("available");
            if (Boolean.TRUE.equals(available)) {
                booking.setRoomId(roomId);
                booking.setHotelId(getLongValue(room.get("hotelId")));
                log.info("Successfully confirmed room: {} for booking: {}", roomId, booking.getId());
                return true;
            } else {
                log.warn("Room {} not available: {}", roomId, response.get("message"));
            }
        }

        return false;
    }

    private boolean confirmSpecificRoom(Booking booking, Long roomId) {
        log.info("Confirming specific room: {} for booking: {}", roomId, booking.getId());

        Map<String, Object> response = hotelServiceClient.confirmAvailability(
                roomId,
                booking.getRequestId(),
                booking.getId(),
                booking.getStartDate(),
                booking.getEndDate()
        );

        Boolean available = (Boolean) response.get("available");
        if (Boolean.TRUE.equals(available)) {
            booking.setRoomId(roomId);
            log.info("Successfully confirmed room: {} for booking: {}", roomId, booking.getId());
            return true;
        } else {
            log.warn("Room {} not available: {}", roomId, response.get("message"));
            return false;
        }
    }

    @Transactional
    protected void compensateBooking(Booking booking, String reason) {
        log.info("Compensating booking: {}, reason: {}", booking.getId(), reason);

        // Снятие резервации в Hotel Service, если была установлена
        if (booking.getRoomId() != null) {
            try {
                hotelServiceClient.releaseReservation(booking.getRoomId(), booking.getRequestId());
            } catch (Exception e) {
                log.error("Failed to release reservation for booking: {}", booking.getId(), e);
            }
        }

        booking.setStatus(Booking.BookingStatus.COMPENSATED);
        booking.setCompensationReason(reason);
        bookingRepository.save(booking);

        log.info("Booking compensated: id={}", booking.getId());
    }

    @Transactional(readOnly = true)
    public List<BookingDto> getUserBookings(Long userId) {
        log.info("Fetching bookings for user: {}", userId);
        List<Booking> bookings = bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return bookings.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BookingDto getBookingById(Long id) {
        log.info("Fetching booking: {}", id);
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        return toDto(booking);
    }

    @Transactional
    public void cancelBooking(Long id) {
        log.info("Cancelling booking: {}", id);

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() == Booking.BookingStatus.CANCELLED ||
            booking.getStatus() == Booking.BookingStatus.COMPENSATED) {
            log.warn("Booking already cancelled/compensated: {}", id);
            return;
        }

        // Снятие резервации в Hotel Service
        if (booking.getRoomId() != null) {
            try {
                hotelServiceClient.releaseReservation(booking.getRoomId(), booking.getRequestId());
            } catch (Exception e) {
                log.error("Failed to release reservation for booking: {}", id, e);
            }
        }

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        log.info("Booking cancelled: {}", id);
    }

    @Transactional
    public void cleanupExpiredBookings() {
        log.info("Cleaning up expired bookings");

        LocalDateTime now = LocalDateTime.now();
        List<Booking> expired = bookingRepository.findExpiredPendingBookings(now);

        for (Booking booking : expired) {
            compensateBooking(booking, "Booking expired");
        }

        log.info("Cleaned up {} expired bookings", expired.size());
    }

    private BookingDto toDto(Booking booking) {
        BookingDto dto = new BookingDto();
        dto.setId(booking.getId());
        dto.setRequestId(booking.getRequestId());
        dto.setUserId(booking.getUserId());
        dto.setHotelId(booking.getHotelId());
        dto.setRoomId(booking.getRoomId());
        dto.setStartDate(booking.getStartDate());
        dto.setEndDate(booking.getEndDate());
        dto.setStatus(booking.getStatus().name());
        dto.setCreatedAt(booking.getCreatedAt());
        dto.setExpiresAt(booking.getExpiresAt());
        dto.setCompensationReason(booking.getCompensationReason());
        return dto;
    }

    private Long getLongValue(Object value) {
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof String) return Long.parseLong((String) value);
        return null;
    }
}

