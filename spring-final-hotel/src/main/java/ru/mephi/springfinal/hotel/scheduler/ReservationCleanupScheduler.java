package ru.mephi.springfinal.hotel.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.mephi.springfinal.hotel.service.RoomService;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationCleanupScheduler {

    private final RoomService roomService;

    @Scheduled(fixedDelayString = "${reservation.cleanup.interval:60000}") // default: every minute
    public void cleanupExpiredReservations() {
        log.debug("Running reservation cleanup task");
        roomService.cleanupExpiredReservations();
    }
}

