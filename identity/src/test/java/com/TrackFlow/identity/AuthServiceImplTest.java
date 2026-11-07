package com.TrackFlow.identity;

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
import com.TrackFlow.identity.services.JwtService;
import com.TrackFlow.identity.services.impl.AuthServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;

    @InjectMocks private AuthServiceImpl authService;

    private RegisterRequest registerRequest() {
        return new RegisterRequest(
                "Saad", "Ait", "AB123456", "saad@example.com", "password123",
                "+212600000000", LocalDate.of(2000, 1, 1), "Marrakech"
        );
    }

    // ---------------------------------------------------------------- register

    @Test
    @DisplayName("register: persists user with encoded password and CUSTOMER role")
    void register_success() {
        RegisterRequest request = registerRequest();
        UUID id = UUID.randomUUID();

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.existsByCin(request.cin())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("ENCODED");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(id);
            return u;
        });

        RegisterResponse response = authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getPassword()).isEqualTo("ENCODED").isNotEqualTo(request.password());
        assertThat(saved.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(saved.getFirstName()).isEqualTo(request.firstName());
        assertThat(saved.getLastName()).isEqualTo(request.lastName());
        assertThat(saved.getCin()).isEqualTo(request.cin());
        assertThat(saved.getEmail()).isEqualTo(request.email());
        assertThat(saved.getPhoneNumber()).isEqualTo(request.phoneNumber());
        assertThat(saved.getBirthday()).isEqualTo(request.birthday());
        assertThat(saved.getAddress()).isEqualTo(request.address());

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.email()).isEqualTo(request.email());
        assertThat(response.firstName()).isEqualTo(request.firstName());
        assertThat(response.lastName()).isEqualTo(request.lastName());
        assertThat(response.role()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    @DisplayName("register: throws when email already exists and never saves")
    void register_emailExists() {
        RegisterRequest request = registerRequest();
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessage("Email already exists");

        verify(userRepository, never()).existsByCin(any());
        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("register: throws when CIN already exists and never saves")
    void register_cinExists() {
        RegisterRequest request = registerRequest();
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.existsByCin(request.cin())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(CinAlreadyExistsException.class)
                .hasMessage("CIN already exists");

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    // ------------------------------------------------------------------- login

    @Test
    @DisplayName("login: authenticates and returns a bearer token")
    void login_success() {
        LoginRequest request = new LoginRequest("saad@example.com", "password123");
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        Authentication authentication = mock(Authentication.class);
        Instant expiration = Instant.now().plusSeconds(3600);

        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");
        when(jwtService.getExpirationInSeconds()).thenReturn(3600L);
        when(jwtService.extractExpirationInstant("jwt-token")).thenReturn(expiration);

        LoginResponse response = authService.login(request);

        ArgumentCaptor<Authentication> captor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(captor.getValue().getPrincipal()).isEqualTo(request.email());
        assertThat(captor.getValue().getCredentials()).isEqualTo(request.password());
        assertThat(captor.getValue().isAuthenticated()).isFalse();

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600L);
        assertThat(response.expirationInstant()).isEqualTo(expiration);
    }

    @Test
    @DisplayName("login: propagates BadCredentialsException and does not generate a token")
    void login_badCredentials() {
        LoginRequest request = new LoginRequest("saad@example.com", "wrong");
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(jwtService);
    }
}