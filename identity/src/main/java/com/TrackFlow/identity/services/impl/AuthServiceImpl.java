package com.TrackFlow.identity.services.impl;



import com.TrackFlow.identity.dtos.request.LoginRequest;
import com.TrackFlow.identity.dtos.request.RegisterRequest;
import com.TrackFlow.identity.dtos.response.LoginResponse;
import com.TrackFlow.identity.dtos.response.RegisterResponse;
import com.TrackFlow.identity.entities.CustomUserDetails;
import com.TrackFlow.identity.entities.User;
import com.TrackFlow.identity.entities.enums.Role;
import com.TrackFlow.identity.exceptions.exceptions.CinAlreadyExistsException;
import com.TrackFlow.identity.exceptions.exceptions.EmailAlreadyExistsException;
import com.TrackFlow.identity.repositories.UserRepository;
import com.TrackFlow.identity.services.AuthService;
import com.TrackFlow.identity.services.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        log.info("Registration attempt for email: {}", request.email());

        if (userRepository.existsByEmail(request.email())) {
            log.warn("Registration failed: email already exists");
            throw new EmailAlreadyExistsException("Email already exists");
        }

        if (userRepository.existsByCin(request.cin())) {
            log.warn("Registration failed: CIN already exists");
            throw new CinAlreadyExistsException("CIN already exists");
        }

        User user = new User();

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setCin(request.cin());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPhoneNumber(request.phoneNumber());
        user.setBirthday(request.birthday());
        user.setAddress(request.address());
        user.setRole(Role.CUSTOMER);

        User savedUser = userRepository.save(user);

        log.info("User registered successfully with id: {}", savedUser.getId());

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getEmail(),
                savedUser.getRole()
        );
    }

    @Override
    public LoginResponse login(LoginRequest request) {

        log.info("Login attempt for email: {}", request.email());

        Authentication noAuthenticatedAuthentication =  new UsernamePasswordAuthenticationToken(
                request.email(),
                request.password()
        );
        Authentication authentication = authenticationManager.authenticate(
                noAuthenticatedAuthentication
        );

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        String token = jwtService.generateToken(userDetails);

        log.info("User authenticated successfully: {}", request.email());

        return new LoginResponse(
                token,
                "Bearer",
                jwtService.getExpirationInSeconds(),
                jwtService.extractExpirationInstant(token)
        );
    }
}