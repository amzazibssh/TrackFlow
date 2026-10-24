package com.TrackFlow.shippment.dtos.requests;

import com.TrackFlow.shippment.entities.enums.ShipmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateStatusRequest(
        @NotNull UUID id,
        @NotNull ShipmentStatus status) {
}
