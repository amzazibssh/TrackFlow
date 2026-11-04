package com.TrackFlow.shipment.dtos.requests;

import com.TrackFlow.shipment.dtos.entitiesDTO.CustomerDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record CreateShipmentRequest(
        @NotBlank @Size(max = 255) String originAddress,
        @NotBlank @Size(max = 255) String destinationAddress,
        @NotNull @FutureOrPresent LocalDate pickupDate,
        @NotNull @Positive Double weightKg,
        @NotNull @Valid CustomerDTO customerDTO
        ) {
}
