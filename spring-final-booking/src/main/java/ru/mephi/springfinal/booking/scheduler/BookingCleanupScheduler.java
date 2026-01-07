package ru.mephi.springfinal.booking.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.mephi.springfinal.booking.service.BookingService;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingCleanupScheduler {

    private final BookingService bookingService;

    @Scheduled(fixedDelayString = "${booking.cleanup.interval:60000}") // default: every minute
    public void cleanupExpiredBookings() {
        log.debug("Running booking cleanup task");
        bookingService.cleanupExpiredBookings();
    }
}

