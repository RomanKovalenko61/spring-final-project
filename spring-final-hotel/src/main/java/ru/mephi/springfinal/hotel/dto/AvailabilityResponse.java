package ru.mephi.springfinal.hotel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityResponse {
    private boolean available;
    private String message;
    private Long roomId;

    public static AvailabilityResponse success(Long roomId) {
        return new AvailabilityResponse(true, "Room is available and reserved", roomId);
    }

    public static AvailabilityResponse failure(String message) {
        return new AvailabilityResponse(false, message, null);
    }
}

