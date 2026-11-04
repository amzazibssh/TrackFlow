package com.TrackFlow.shipment.dtos.entitiesDTO;

import com.TrackFlow.shipment.entities.enums.ShipmentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ShipmentDTO(
        String trackingNumber,
        String originAddress,
        String destinationAddress,
        LocalDate pickupDate,
        LocalDate estimatedDeliveryDate,
        ShipmentStatus status,
        BigDecimal weightKg,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {
}