package ru.mephi.springfinal.hotel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.mephi.springfinal.hotel.entity.Room;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByHotelId(Long hotelId);

    @Query("SELECT r FROM Room r WHERE r.available = true AND r.id NOT IN " +
           "(SELECT rr.roomId FROM RoomReservation rr WHERE rr.status IN ('PENDING', 'CONFIRMED') " +
           "AND rr.startDate < :endDate AND rr.endDate > :startDate)")
    List<Room> findAvailableRooms(@Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate);

    @Query("SELECT r FROM Room r WHERE r.available = true AND r.type = :type " +
           "AND r.id NOT IN (SELECT rr.roomId FROM RoomReservation rr " +
           "WHERE rr.status IN ('PENDING', 'CONFIRMED') " +
           "AND rr.startDate < :endDate AND rr.endDate > :startDate) " +
           "ORDER BY r.timesBooked ASC, r.id ASC")
    List<Room> findAvailableRoomsByTypeRecommended(@Param("type") Room.RoomType type,
                                                    @Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate);

    @Query("SELECT r FROM Room r WHERE r.hotel.id = :hotelId AND r.available = true " +
           "AND r.id NOT IN (SELECT rr.roomId FROM RoomReservation rr " +
           "WHERE rr.status IN ('PENDING', 'CONFIRMED') " +
           "AND rr.startDate < :endDate AND rr.endDate > :startDate) " +
           "ORDER BY r.timesBooked ASC, r.id ASC")
    List<Room> findAvailableRoomsByHotelRecommended(@Param("hotelId") Long hotelId,
                                                     @Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate);
}

