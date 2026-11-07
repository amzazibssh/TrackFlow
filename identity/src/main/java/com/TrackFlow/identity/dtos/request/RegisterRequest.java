package com.TrackFlow.identity.dtos.request;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record RegisterRequest(

        @NotBlank
        @Size(max = 30)
        String firstName,

        @NotBlank
        @Size(max = 30)
        String lastName,

        @NotBlank
        @Size(max = 20)
        String cin,

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 8, max = 72)
        String password,

        @Pattern(regexp = "^\\+?[0-9]{8,15}$")
        String phoneNumber,

        @Past
        LocalDate birthday,

        @Size(max = 100)
        String address
) {}
