package com.TrackFlow.shipment.dtos.requests;

import com.TrackFlow.shipment.entities.enums.ShipmentStatus;
import jakarta.validation.constraints.NotNull;


public record UpdateStatusRequest(
        @NotNull String trackingNumber,
        @NotNull ShipmentStatus status) {
}
