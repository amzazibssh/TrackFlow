package com.TrackFlow.shipment.events;

import com.TrackFlow.shipment.entities.enums.ShipmentStatus;

import java.time.Instant;
import java.util.UUID;

public record ShipmentStatusChangedEvent(
        UUID eventId,
        String trackingNumber,
        UUID customerId,
        ShipmentStatus previousStatus,
        ShipmentStatus newStatus,
        Instant occurredAt
) {
}
