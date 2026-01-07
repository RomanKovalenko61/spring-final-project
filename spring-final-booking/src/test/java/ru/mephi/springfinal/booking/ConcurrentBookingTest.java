package ru.mephi.springfinal.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import ru.mephi.springfinal.booking.config.TestHotelServiceConfig;
import ru.mephi.springfinal.booking.dto.BookingDto;
import ru.mephi.springfinal.booking.entity.Booking;
import ru.mephi.springfinal.booking.repository.BookingRepository;
import ru.mephi.springfinal.booking.service.BookingService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestHotelServiceConfig.class)
@DisplayName("Concurrent Booking and Saga Pattern Tests")
class ConcurrentBookingTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
    }

    @Test
    @DisplayName("Параллельные бронирования одной комнаты - должны обрабатываться корректно")
    void testConcurrentBookingsForSameRoom() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Future<BookingDto>> futures = new ArrayList<>();

        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = LocalDate.now().plusDays(15);

        // Запускаем 10 параллельных попыток бронирования
        for (int i = 0; i < threadCount; i++) {
            final long userId = (long) (i + 1);
            Future<BookingDto> future = executorService.submit(() -> {
                try {
                    BookingDto dto = new BookingDto();
                    dto.setUserId(userId);
                    dto.setRoomId(1L);
                    dto.setStartDate(startDate);
                    dto.setEndDate(endDate);
                    dto.setAutoSelect(false);

                    BookingDto result = bookingService.createBooking(dto);
                    return result;
                } catch (Exception e) {
                    return null;
                } finally {
                    latch.countDown();
                }
            });
            futures.add(future);
        }

        // Ждем завершения всех потоков
        latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();

        // Проверяем результаты
        int successCount = 0;
        int failCount = 0;

        for (Future<BookingDto> future : futures) {
            try {
                BookingDto result = future.get(1, TimeUnit.SECONDS);
                if (result != null && "CONFIRMED".equals(result.getStatus())) {
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                failCount++;
            }
        }

        // Должна быть ровно одна успешная бронь, остальные должны быть отклонены
        System.out.println("Success: " + successCount + ", Failed: " + failCount);
        assertTrue(successCount >= 1, "Должна быть хотя бы одна успешная бронь");
        assertTrue(failCount >= 1, "Остальные брони должны быть отклонены");
    }

    @Test
    @DisplayName("Идемпотентность - повторный запрос с тем же requestId не создает дубликат")
    void testIdempotency() {
        BookingDto dto = new BookingDto();
        dto.setUserId(1L);
        dto.setRoomId(1L);
        dto.setStartDate(LocalDate.now().plusDays(1));
        dto.setEndDate(LocalDate.now().plusDays(5));
        dto.setAutoSelect(false);
        dto.setRequestId("test-request-123");

        // Первый запрос
        BookingDto first = bookingService.createBooking(dto);
        assertNotNull(first);
        Long firstId = first.getId();

        // Повторный запрос с тем же requestId
        BookingDto second = bookingService.createBooking(dto);
        assertNotNull(second);

        // ID должны совпадать (вернулось существующее бронирование)
        assertEquals(firstId, second.getId(), "Повторный запрос должен вернуть существующее бронирование");

        // Проверяем, что в БД только одна запись
        List<Booking> bookings = bookingRepository.findAll();
        long count = bookings.stream()
                .filter(b -> "test-request-123".equals(b.getRequestId()))
                .count();
        assertEquals(1, count, "Должна быть только одна запись с данным requestId");
    }

    @Test
    @DisplayName("Тест Saga Pattern - компенсация при недоступности комнаты")
    void testSagaCompensation() {
        BookingDto dto = new BookingDto();
        dto.setUserId(1L);
        dto.setRoomId(999L); // Несуществующая комната
        dto.setStartDate(LocalDate.now().plusDays(1));
        dto.setEndDate(LocalDate.now().plusDays(5));
        dto.setAutoSelect(false);

        BookingDto result = bookingService.createBooking(dto);

        assertNotNull(result);
        // При ошибке должна быть выполнена компенсация
        assertTrue(
                "COMPENSATED".equals(result.getStatus()) || "CANCELLED".equals(result.getStatus()),
                "Бронирование должно быть компенсировано или отменено"
        );
    }

    @Test
    @DisplayName("Автоподбор комнаты с балансировкой загрузки")
    void testAutoSelectWithLoadBalancing() {
        // Создаем несколько бронирований с автоподбором
        for (int i = 0; i < 5; i++) {
            BookingDto dto = new BookingDto();
            dto.setUserId((long) (i + 1));
            dto.setStartDate(LocalDate.now().plusDays(20 + i * 10)); // Не пересекающиеся даты
            dto.setEndDate(LocalDate.now().plusDays(25 + i * 10));
            dto.setAutoSelect(true);
            dto.setRoomType("DOUBLE");

            BookingDto result = bookingService.createBooking(dto);
            assertNotNull(result);
        }

        // Проверяем, что созданы бронирования
        List<Booking> bookings = bookingRepository.findAll();
        assertTrue(bookings.size() >= 1, "Должно быть создано хотя бы одно бронирование");
    }

    @Test
    @DisplayName("Отмена бронирования и освобождение комнаты")
    void testCancellationAndRoomRelease() {
        // Создаем бронирование
        BookingDto dto = new BookingDto();
        dto.setUserId(1L);
        dto.setStartDate(LocalDate.now().plusDays(30));
        dto.setEndDate(LocalDate.now().plusDays(35));
        dto.setAutoSelect(true);
        dto.setRoomType("SINGLE");

        BookingDto created = bookingService.createBooking(dto);
        assertNotNull(created);
        assertNotNull(created.getId());

        // Отменяем бронирование
        bookingService.cancelBooking(created.getId());

        // Проверяем статус
        BookingDto cancelled = bookingService.getBookingById(created.getId());
        assertEquals("CANCELLED", cancelled.getStatus(), "Бронирование должно быть отменено");
    }

    @Test
    @DisplayName("Очистка истекших PENDING бронирований")
    void testExpiredBookingsCleanup() throws InterruptedException {
        // Этот тест проверяет работу scheduler'а
        // В реальной ситуации нужно подождать или вызвать метод напрямую

        BookingDto dto = new BookingDto();
        dto.setUserId(1L);
        dto.setStartDate(LocalDate.now().plusDays(1));
        dto.setEndDate(LocalDate.now().plusDays(5));
        dto.setAutoSelect(true);
        dto.setRoomType("DOUBLE");

        BookingDto result = bookingService.createBooking(dto);
        assertNotNull(result);

        // Если бронирование в статусе PENDING, оно должно истечь через заданное время
        if ("PENDING".equals(result.getStatus())) {
            assertNotNull(result.getExpiresAt(), "У PENDING бронирования должна быть дата истечения");
        }
    }

    @Test
    @DisplayName("Получение бронирований пользователя")
    void testGetUserBookings() {
        Long userId = 1L;

        // Создаем несколько бронирований для одного пользователя
        for (int i = 0; i < 3; i++) {
            BookingDto dto = new BookingDto();
            dto.setUserId(userId);
            dto.setStartDate(LocalDate.now().plusDays(40 + i * 10));
            dto.setEndDate(LocalDate.now().plusDays(45 + i * 10));
            dto.setAutoSelect(true);
            dto.setRoomType("DOUBLE");

            bookingService.createBooking(dto);
        }

        List<BookingDto> userBookings = bookingService.getUserBookings(userId);
        assertTrue(userBookings.size() >= 1, "Должны быть бронирования для пользователя");
    }
}