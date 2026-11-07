package com.TrackFlow.identity.dtos.exceptions;

import java.time.Instant;

public record ExceptionDTO(
        Instant date,
        int status,
        String error,
        String message
) {}