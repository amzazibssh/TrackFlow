package com.TrackFlow.shipment.dtos.response;

import com.TrackFlow.shipment.entities.enums.ShipmentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ShipmentResponse(
        String trackingNumber,
        String originAddress,
        String destinationAddress,
        LocalDate pickupDate,
        LocalDate estimatedDeliveryDate,
        ShipmentStatus status,
        BigDecimal weightKg,
        Instant createdAt,
        Instant updatedAt,
        UUID customerId
) {
}
