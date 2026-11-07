package com.TrackFlow.identity;

import com.TrackFlow.identity.controllers.AuthController;
import com.TrackFlow.identity.dtos.request.LoginRequest;
import com.TrackFlow.identity.dtos.request.RegisterRequest;
import com.TrackFlow.identity.dtos.response.LoginResponse;
import com.TrackFlow.identity.dtos.response.RegisterResponse;
import com.TrackFlow.identity.entities.enums.Role;
import com.TrackFlow.identity.exceptions.GlobalExceptionHandler;
import com.TrackFlow.identity.exceptions.exceptions.CinAlreadyExistsException;
import com.TrackFlow.identity.exceptions.exceptions.EmailAlreadyExistsException;
import com.TrackFlow.identity.services.AuthService;
import com.TrackFlow.identity.services.JwtService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller slice: security filters are disabled (covered by the integration test),
 * the service is mocked. JwtService / UserDetailsService are mocked only because
 * @WebMvcTest still instantiates the JwtAuthenticationFilter @Component.
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    private static final String REGISTER = "/v1/api/auth/register";
    private static final String LOGIN = "/v1/api/auth/login";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AuthService authService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;

    private RegisterRequest validRegister() {
        return new RegisterRequest("Saad", "Ait", "AB123456", "saad@example.com",
                "password123", "+212600000000", LocalDate.of(2000, 1, 1), "Marrakech");
    }

    // ---------------------------------------------------------------- register

    @Test
    @DisplayName("POST /register -> 201 with RegisterResponse")
    void register_created() throws Exception {
        UUID id = UUID.randomUUID();
        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(new RegisterResponse(id, "Saad", "Ait", "saad@example.com", Role.CUSTOMER));

        mockMvc.perform(post(REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegister())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.email").value("saad@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("POST /register with invalid fields -> 400 listing the fields")
    void register_validationError() throws Exception {
        RegisterRequest invalid = new RegisterRequest("", "Ait", "AB123456", "not-an-email",
                "short", "abc", LocalDate.now().plusDays(1), "Marrakech");

        mockMvc.perform(post(REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("firstName")))
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("email")))
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("password")))
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("phoneNumber")))
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("birthday")));

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("POST /register with malformed JSON -> 400")
    void register_malformedJson() throws Exception {
        mockMvc.perform(post(REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or unreadable request body"));
    }

    @Test
    @DisplayName("POST /register with duplicate email -> 409")
    void register_emailConflict() throws Exception {
        when(authService.register(any())).thenThrow(new EmailAlreadyExistsException("Email already exists"));

        mockMvc.perform(post(REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegister())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    @Test
    @DisplayName("POST /register with duplicate CIN -> 409")
    void register_cinConflict() throws Exception {
        when(authService.register(any())).thenThrow(new CinAlreadyExistsException("CIN already exists"));

        mockMvc.perform(post(REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegister())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("CIN already exists"));
    }

    @Test
    @DisplayName("POST /register with wrong content type -> 415")
    void register_unsupportedMediaType() throws Exception {
        mockMvc.perform(post(REGISTER).contentType(MediaType.TEXT_PLAIN).content("hello"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("GET /register -> 405")
    void register_methodNotAllowed() throws Exception {
        mockMvc.perform(get(REGISTER)).andExpect(status().isMethodNotAllowed());
    }

    // ------------------------------------------------------------------- login

    @Test
    @DisplayName("POST /login -> 200 with token")
    void login_ok() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new LoginResponse("jwt", "Bearer", 3600L, Instant.now().plusSeconds(3600)));

        mockMvc.perform(post(LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("saad@example.com", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    @DisplayName("POST /login with blank password / bad email -> 400")
    void login_validationError() throws Exception {
        mockMvc.perform(post(LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("not-an-email", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("email")))
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("password")));

        verifyNoInteractions(authService);
    }
}