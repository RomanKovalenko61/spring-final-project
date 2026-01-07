package ru.mephi.springfinal.booking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingDto {
    private Long id;
    private String requestId;

    // userId устанавливается автоматически из аутентификации в контроллере
    private Long userId;

    private Long hotelId;
    private Long roomId;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private Boolean autoSelect = false;
    private String roomType;

    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String compensationReason;
}

