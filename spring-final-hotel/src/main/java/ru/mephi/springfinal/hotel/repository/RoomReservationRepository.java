package ru.mephi.springfinal.hotel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.mephi.springfinal.hotel.entity.RoomReservation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomReservationRepository extends JpaRepository<RoomReservation, Long> {

    Optional<RoomReservation> findByRequestId(String requestId);

    List<RoomReservation> findByBookingId(Long bookingId);

    @Query("SELECT rr FROM RoomReservation rr WHERE rr.status = 'PENDING' " +
           "AND rr.expiresAt < :now")
    List<RoomReservation> findExpiredReservations(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE RoomReservation rr SET rr.status = 'EXPIRED' " +
           "WHERE rr.status = 'PENDING' AND rr.expiresAt < :now")
    int expireOldReservations(@Param("now") LocalDateTime now);
}

