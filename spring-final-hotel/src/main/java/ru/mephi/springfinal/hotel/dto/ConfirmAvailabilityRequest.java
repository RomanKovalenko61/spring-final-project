package ru.mephi.springfinal.hotel.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmAvailabilityRequest {
    @NotNull(message = "Request ID is required")
    private String requestId;

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;
}

