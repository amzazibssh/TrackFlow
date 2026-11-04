package com.TrackFlow.shipment.events;

import com.TrackFlow.shipment.entities.enums.ShipmentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ShipmentCreatedEvent(
        UUID eventId,
        String trackingNumber,
        UUID customerId,
        String originAddress,
        String destinationAddress,
        LocalDate pickupDate,
        BigDecimal weightKg,
        ShipmentStatus status,
        Instant occurredAt
) {}