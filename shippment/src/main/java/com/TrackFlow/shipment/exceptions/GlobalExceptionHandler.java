package com.TrackFlow.shipment.exceptions;

import com.TrackFlow.shipment.dtos.exceptions.ExceptionDTO;
import com.TrackFlow.shipment.exceptions.exceptons.InvalidStatusTransitionException;
import com.TrackFlow.shipment.exceptions.exceptons.ShipmentNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import org.springframework.security.access.AccessDeniedException;
import java.time.Instant;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ExceptionDTO> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ExceptionDTO(Instant.now(), "Malformed or unreadable request body"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionDTO> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ExceptionDTO(Instant.now(), message));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ExceptionDTO> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ExceptionDTO(Instant.now(), "HTTP method not supported for this endpoint"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ExceptionDTO> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(new ExceptionDTO(Instant.now(), "Content type not supported, use application/json"));
    }

    @ExceptionHandler(ShipmentNotFoundException.class)
    public ResponseEntity<ExceptionDTO> handleShipmentNotFound(ShipmentNotFoundException ex){
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ExceptionDTO(Instant.now(), ex.getMessage()));
    }


    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ExceptionDTO> handleInvalidStatusTransition(InvalidStatusTransitionException ex){
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ExceptionDTO(Instant.now(), ex.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ExceptionDTO> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ExceptionDTO(Instant.now(), "Endpoint not found"));
    }


    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ExceptionDTO> handleAuthorizationDenied(AuthorizationDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ExceptionDTO(Instant.now(), "Access denied"));
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<ExceptionDTO> handleDownstreamClientError(HttpClientErrorException ex) {
        log.warn("Identity service returned {}", ex.getStatusCode());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ExceptionDTO(Instant.now(), "Identity service call failed"));
    }

    @ExceptionHandler(ResourceAccessException.class)   // identity service down / timeout
    public ResponseEntity<ExceptionDTO> handleDownstreamUnavailable(ResourceAccessException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ExceptionDTO(Instant.now(), "Identity service unavailable"));
    }


    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ExceptionDTO> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.error("Database integrity violation", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ExceptionDTO(Instant.now(), "Database constraint violation"));
    }


    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ExceptionDTO> handleAccessDenied(AccessDeniedException ex){
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ExceptionDTO(Instant.now(), ex.getMessage()));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionDTO> handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ExceptionDTO(Instant.now(), "Unknown error"));
    }
}