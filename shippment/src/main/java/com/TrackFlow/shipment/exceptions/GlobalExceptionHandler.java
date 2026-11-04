package com.TrackFlow.shipment.exceptions;

import com.TrackFlow.shipment.dtos.exceptions.ExceptionDTO;
import com.TrackFlow.shipment.exceptions.exceptons.InvalidStatusTransitionException;
import com.TrackFlow.shipment.exceptions.exceptons.ShipmentNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.file.AccessDeniedException;
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