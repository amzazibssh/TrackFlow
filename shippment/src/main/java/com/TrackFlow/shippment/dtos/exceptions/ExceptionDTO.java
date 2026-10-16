package com.TrackFlow.shippment.dtos.exceptions;

import java.time.Instant;

public record ExceptionDTO(
        Instant date,
        String message
) {
}