package com.TrackFlow.identity;

import com.TrackFlow.identity.dtos.exceptions.ExceptionDTO;
import com.TrackFlow.identity.exceptions.GlobalExceptionHandler;
import com.TrackFlow.identity.exceptions.exceptions.CinAlreadyExistsException;
import com.TrackFlow.identity.exceptions.exceptions.EmailAlreadyExistsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private void assertBody(ResponseEntity<ExceptionDTO> response, HttpStatus status, String message) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        ExceptionDTO body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.date()).isNotNull();
        assertThat(body.status()).isEqualTo(status.value());
        assertThat(body.error()).isEqualTo(status.getReasonPhrase());
        assertThat(body.message()).isEqualTo(message);
    }

    @Test
    @DisplayName("validation errors are joined as 'field: message' with 400")
    void handleValidation() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "registerRequest");
        bindingResult.addError(new FieldError("registerRequest", "email", "must be a well-formed email address"));
        bindingResult.addError(new FieldError("registerRequest", "password", "size must be between 8 and 72"));

        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult);

        assertBody(handler.handleValidation(ex), HttpStatus.BAD_REQUEST,
                "email: must be a well-formed email address, password: size must be between 8 and 72");
    }

    @Test
    @DisplayName("unreadable body -> 400")
    void handleUnreadableMessage() {
        var ex = new HttpMessageNotReadableException("boom", mock(HttpInputMessage.class));

        assertBody(handler.handleUnreadableMessage(ex), HttpStatus.BAD_REQUEST,
                "Malformed or unreadable request body");
    }

    @Test
    @DisplayName("unsupported method -> 405")
    void handleMethodNotSupported() {
        var ex = new HttpRequestMethodNotSupportedException("GET");

        assertBody(handler.handleMethodNotSupported(ex), HttpStatus.METHOD_NOT_ALLOWED,
                "HTTP method not supported");
    }

    @Test
    @DisplayName("unsupported media type -> 415")
    void handleMediaTypeNotSupported() {
        var ex = new HttpMediaTypeNotSupportedException("text/plain not supported");

        assertBody(handler.handleMediaTypeNotSupported(ex), HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Content type not supported");
    }

    @Test
    @DisplayName("duplicate email -> 409 with exception message")
    void handleEmailExists() {
        assertBody(handler.handleEmailExists(new EmailAlreadyExistsException("Email already exists")),
                HttpStatus.CONFLICT, "Email already exists");
    }

    @Test
    @DisplayName("duplicate CIN -> 409 with exception message")
    void handleCinExists() {
        assertBody(handler.handleCinExists(new CinAlreadyExistsException("CIN already exists")),
                HttpStatus.CONFLICT, "CIN already exists");
    }

    @Test
    @DisplayName("unexpected exception -> 500 and internal details are not leaked")
    void handleUnexpected() {
        var response = handler.handleUnexpectedException(new RuntimeException("secret db password leaked"));

        assertBody(response, HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        assertThat(response.getBody().message()).doesNotContain("secret");
    }
}