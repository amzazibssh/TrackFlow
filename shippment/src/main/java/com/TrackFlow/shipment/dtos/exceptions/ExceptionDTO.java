package com.TrackFlow.shipment.dtos.exceptions;

import java.time.Instant;

public record ExceptionDTO(
        Instant date,
        String message
) {
}