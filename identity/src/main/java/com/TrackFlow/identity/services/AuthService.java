package com.TrackFlow.identity.services;

import com.TrackFlow.identity.dtos.request.LoginRequest;
import com.TrackFlow.identity.dtos.request.RegisterRequest;
import com.TrackFlow.identity.dtos.response.LoginResponse;
import com.TrackFlow.identity.dtos.response.RegisterResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    RegisterResponse register(RegisterRequest request);
}
