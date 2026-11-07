package com.TrackFlow.identity;

import com.TrackFlow.identity.dtos.request.LoginRequest;
import com.TrackFlow.identity.dtos.request.RegisterRequest;
import com.TrackFlow.identity.entities.User;
import com.TrackFlow.identity.entities.enums.Role;
import com.TrackFlow.identity.repositories.UserRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full stack: real security filter chain, real BCrypt, real JWT, in-memory H2.
 * Config comes from src/test/resources/application-test.yml.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    private static final String REGISTER = "/v1/api/auth/register";
    private static final String LOGIN = "/v1/api/auth/login";
    private static final String PROTECTED = "/v1/api/some-protected-resource";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;

    @BeforeEach
    void cleanDb() {
        userRepository.deleteAll();
    }

    private RegisterRequest registerRequest(String email, String cin) {
        return new RegisterRequest("Saad", "Ait", cin, email, "password123",
                "+212600000000", LocalDate.of(2000, 1, 1), "Marrakech");
    }

    private void register(RegisterRequest request) throws Exception {
        mockMvc.perform(post(REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post(LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("accessToken").asText();
    }

    // ---------------------------------------------------------------- register

    @Test
    @DisplayName("register persists a CUSTOMER with a BCrypt-hashed password")
    void register_persistsUser() throws Exception {
        mockMvc.perform(post(REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest("saad@example.com", "AB123456"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.email").value("saad@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.password").doesNotExist());

        User saved = userRepository.findAll().get(0);
        assertThat(saved.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(saved.getPassword()).isNotEqualTo("password123").startsWith("$2");
        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("register with duplicate email -> 409")
    void register_duplicateEmail() throws Exception {
        register(registerRequest("saad@example.com", "AB123456"));

        mockMvc.perform(post(REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest("saad@example.com", "ZZ999999"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already exists"));

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("register with duplicate CIN -> 409")
    void register_duplicateCin() throws Exception {
        register(registerRequest("saad@example.com", "AB123456"));

        mockMvc.perform(post(REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest("other@example.com", "AB123456"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("CIN already exists"));

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("register with invalid body -> 400 and nothing persisted")
    void register_invalid() throws Exception {
        RegisterRequest invalid = new RegisterRequest("Saad", "Ait", "AB123456", "bad-email",
                "short", null, null, null);

        mockMvc.perform(post(REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());

        assertThat(userRepository.count()).isZero();
    }

    // ------------------------------------------------------------------- login

    @Test
    @DisplayName("register -> login returns a bearer JWT")
    void login_success() throws Exception {
        register(registerRequest("saad@example.com", "AB123456"));

        mockMvc.perform(post(LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("saad@example.com", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andExpect(jsonPath("$.expirationInstant").isNotEmpty());
    }

    // NOTE: the next two tests assert the *desired* behaviour (401). With the current
    // GlobalExceptionHandler they return 500 because BadCredentialsException falls into
    // the catch-all handler. They will pass once you add the handlers suggested in the reply.

    @Test
    @DisplayName("login with wrong password -> 401")
    void login_wrongPassword() throws Exception {
        register(registerRequest("saad@example.com", "AB123456"));

        mockMvc.perform(post(LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("saad@example.com", "wrong-password"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("login with unknown email -> 401 (no user enumeration)")
    void login_unknownEmail() throws Exception {
        mockMvc.perform(post(LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("ghost@example.com", "password123"))))
                .andExpect(status().isUnauthorized());
    }

    // --------------------------------------------------------- JWT filter chain

    @Test
    @DisplayName("protected endpoint without token -> 401")
    void protected_noToken() throws Exception {
        mockMvc.perform(get(PROTECTED)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("protected endpoint with a valid token passes the security layer")
    void protected_validToken() throws Exception {
        register(registerRequest("saad@example.com", "AB123456"));
        String token = loginAndGetToken("saad@example.com", "password123");

        MvcResult result = mockMvc.perform(get(PROTECTED)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andReturn();

        // No controller exists for this path, so we only assert we were NOT rejected by security.
        assertThat(result.getResponse().getStatus()).isNotIn(401, 403);
    }

    @Test
    @DisplayName("protected endpoint with a garbage token -> 401 (currently 500, see reply)")
    void protected_garbageToken() throws Exception {
        mockMvc.perform(get(PROTECTED)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("token of a deleted user -> 401 (currently 500, see reply)")
    void protected_tokenOfDeletedUser() throws Exception {
        register(registerRequest("saad@example.com", "AB123456"));
        String token = loginAndGetToken("saad@example.com", "password123");
        userRepository.deleteAll();

        mockMvc.perform(get(PROTECTED)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}