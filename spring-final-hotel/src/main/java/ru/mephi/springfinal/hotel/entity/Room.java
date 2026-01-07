package ru.mephi.springfinal.hotel.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "rooms", indexes = {
    @Index(name = "idx_hotel_id", columnList = "hotel_id"),
    @Index(name = "idx_room_number", columnList = "room_number"),
    @Index(name = "idx_times_booked", columnList = "times_booked")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(name = "room_number", nullable = false)
    private String roomNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomType type;

    @Column(name = "price_per_night", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerNight;

    @Column(name = "times_booked", nullable = false)
    private Integer timesBooked = 0;

    @Column(nullable = false)
    private Boolean available = true;

    @Version
    private Integer version;

    public enum RoomType {
        SINGLE,
        DOUBLE,
        SUITE,
        DELUXE
    }
}

