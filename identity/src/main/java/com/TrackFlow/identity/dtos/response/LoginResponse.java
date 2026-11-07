package com.TrackFlow.identity.dtos.response;

import java.time.Instant;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        Instant expirationInstant
) {}
