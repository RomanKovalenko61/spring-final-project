package ru.mephi.springfinal.booking.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import ru.mephi.springfinal.booking.client.HotelServiceClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@TestConfiguration
@Profile("test")
public class TestHotelServiceConfig {

    // roomId -> (requestId -> date range)
    private final Map<Long, Map<String, DateRange>> reservations = new ConcurrentHashMap<>();

    @Bean
    @Primary
    public HotelServiceClient mockHotelServiceClient() {
        HotelServiceClient mock = Mockito.mock(HotelServiceClient.class);

        // Мокируем getRecommendedRooms - возвращаем список доступных комнат как Map
        Map<String, Object> room1 = createMockRoom(1L, 1L, "101", "SINGLE", new BigDecimal("1000"));
        Map<String, Object> room2 = createMockRoom(2L, 1L, "201", "DOUBLE", new BigDecimal("2000"));
        Map<String, Object> room3 = createMockRoom(3L, 2L, "301", "SUITE", new BigDecimal("5000"));

        Mockito.when(mock.getRecommendedRooms(
            Mockito.any(),
            Mockito.anyString(),
            Mockito.any(LocalDate.class),
            Mockito.any(LocalDate.class)
        )).thenAnswer(invocation -> {
            // возвращаем обычный список (не Mono) — метод в клиенте ожидает List<Map<String,Object>>
            return Arrays.asList(room1, room2, room3);
        });

        // Список известных (существующих) комнат в мок-сервисе
        Set<Long> knownRoomIds = Set.of(1L, 2L, 3L);

        // confirmAvailability должна проверять наличие пересечения и быть идемпотентной по requestId
        Mockito.when(mock.confirmAvailability(
                Mockito.anyLong(),
                Mockito.anyString(),
                Mockito.anyLong(),
                Mockito.any(LocalDate.class),
                Mockito.any(LocalDate.class)
        )).thenAnswer(invocation -> {
            Long roomId = invocation.getArgument(0);
            String requestId = invocation.getArgument(1);
            // Long bookingId = invocation.getArgument(2);
            LocalDate start = invocation.getArgument(3);
            LocalDate end = invocation.getArgument(4);

            Map<String, Object> resp = new HashMap<>();

            // Если номер не существует в нашем мок-отеле — отклоняем (available=false)
            if (!knownRoomIds.contains(roomId)) {
                resp.put("available", false);
                resp.put("message", "Room not found");
                return resp;
            }

            // Идемпотентность: если уже есть резервация с таким requestId для этого номера — вернуть available=true
            reservations.putIfAbsent(roomId, new ConcurrentHashMap<>());
            Map<String, DateRange> roomRes = reservations.get(roomId);
            synchronized (roomRes) {
                if (roomRes.containsKey(requestId)) {
                    resp.put("available", true);
                    resp.put("roomId", roomId);
                    return resp;
                }
                // Проверяем пересечение с существующими
                boolean conflict = roomRes.values().stream().anyMatch(range -> range.overlaps(start, end));
                if (!conflict) {
                    // Резервируем
                    roomRes.put(requestId, new DateRange(start, end));
                    resp.put("available", true);
                    resp.put("roomId", roomId);
                } else {
                    resp.put("available", false);
                    resp.put("message", "Room is already booked for the requested dates");
                }
            }
            return resp;
        });

        // releaseReservation: снять резерв по requestId
        Mockito.doAnswer(invocation -> {
            Long roomId = invocation.getArgument(0);
            String requestId = invocation.getArgument(1);
            Map<String, DateRange> roomRes = reservations.get(roomId);
            if (roomRes != null) {
                synchronized (roomRes) {
                    roomRes.remove(requestId);
                }
            }
            return null;
        }).when(mock).releaseReservation(Mockito.anyLong(), Mockito.anyString());

        return mock;
    }

    private Map<String, Object> createMockRoom(Long id, Long hotelId, String number, String type, BigDecimal price) {
        Map<String, Object> room = new HashMap<>();
        room.put("id", id);
        room.put("hotelId", hotelId);
        room.put("roomNumber", number);
        room.put("type", type);
        room.put("pricePerNight", price);
        room.put("available", true);
        room.put("timesBooked", 0);
        return room;
    }

    // Внутренний класс для хранения диапазона дат и проверки перекрытия
    private static class DateRange {
        private final LocalDate start;
        private final LocalDate end;

        DateRange(LocalDate start, LocalDate end) {
            this.start = start;
            this.end = end;
        }

        boolean overlaps(LocalDate otherStart, LocalDate otherEnd) {
            // Пересечение интервалов [start,end) и [otherStart,otherEnd)
            return !(otherEnd.isEqual(start) || otherEnd.isBefore(start) || otherStart.isEqual(end) || otherStart.isAfter(end));
        }
    }
}
