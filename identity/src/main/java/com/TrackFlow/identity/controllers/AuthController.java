package com.TrackFlow.identity.controllers;

import com.TrackFlow.identity.dtos.request.LoginRequest;
import com.TrackFlow.identity.dtos.request.RegisterRequest;
import com.TrackFlow.identity.dtos.response.LoginResponse;
import com.TrackFlow.identity.dtos.response.RegisterResponse;
import com.TrackFlow.identity.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

}