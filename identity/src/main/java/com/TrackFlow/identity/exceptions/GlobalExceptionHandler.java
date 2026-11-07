package com.TrackFlow.identity.exceptions;

import com.TrackFlow.identity.dtos.exceptions.ExceptionDTO;
import com.TrackFlow.identity.exceptions.exceptions.CinAlreadyExistsException;
import com.TrackFlow.identity.exceptions.exceptions.EmailAlreadyExistsException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import io.jsonwebtoken.JwtException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<ExceptionDTO> buildResponse(
            HttpStatus status,
            String message
    ) {
        return ResponseEntity.status(status)
                .body(new ExceptionDTO(
                        Instant.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        message
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionDTO> handleValidation(
            MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ExceptionDTO> handleUnreadableMessage(
            HttpMessageNotReadableException ex) {

        return buildResponse(HttpStatus.BAD_REQUEST,
                "Malformed or unreadable request body");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ExceptionDTO> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex) {

        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED,
                "HTTP method not supported");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ExceptionDTO> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex) {

        return buildResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Content type not supported");
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ExceptionDTO> handleEmailExists(
            EmailAlreadyExistsException ex) {

        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }



    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ExceptionDTO> handleAuthentication(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ExceptionDTO> handleJwt(JwtException ex) {
        log.warn("Invalid JWT: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ExceptionDTO> handleNotFound(NoResourceFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Resource not found");
    }

    @ExceptionHandler(CinAlreadyExistsException.class)
    public ResponseEntity<ExceptionDTO> handleCinExists(
            CinAlreadyExistsException ex) {

        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionDTO> handleUnexpectedException(Exception ex) {

        log.error("Unhandled exception", ex);

        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred");
    }
}