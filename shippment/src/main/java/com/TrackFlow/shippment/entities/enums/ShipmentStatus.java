package com.TrackFlow.shippment.entities.enums;

import java.util.Map;
import java.util.Set;

public enum ShipmentStatus {
    CREATED, PICKED_UP, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, FAILED, CANCELED, RETURNED;

    private static final Map<ShipmentStatus, Set<ShipmentStatus>> ALLOWED = Map.of(
            CREATED, Set.of(PICKED_UP, CANCELED),
            PICKED_UP, Set.of(IN_TRANSIT, FAILED),
            IN_TRANSIT, Set.of(OUT_FOR_DELIVERY, FAILED),
            OUT_FOR_DELIVERY, Set.of(DELIVERED, FAILED),
            FAILED, Set.of(OUT_FOR_DELIVERY, RETURNED)
    );

    public boolean canTransitionTo(ShipmentStatus next) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(next);
    }
}
