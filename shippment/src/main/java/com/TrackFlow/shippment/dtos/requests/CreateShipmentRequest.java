package com.TrackFlow.shippment.dtos.requests;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record CreateShipmentRequest(
        @NotBlank @Size(max = 255) String originAddress,
        @NotBlank @Size(max = 255) String destinationAddress,
        @NotNull @FutureOrPresent LocalDate pickupDate,
        @NotNull @Positive Double weightKg
) {
}
