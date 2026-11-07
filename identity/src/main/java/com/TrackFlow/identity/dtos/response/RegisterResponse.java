package com.TrackFlow.identity.dtos.response;


import com.TrackFlow.identity.entities.enums.Role;

import java.util.UUID;

public record RegisterResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        Role role
) {}
