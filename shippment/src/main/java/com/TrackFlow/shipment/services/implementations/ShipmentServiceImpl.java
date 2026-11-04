package com.TrackFlow.shipment.services.implementations;

import com.TrackFlow.shipment.dtos.requests.CreateShipmentRequest;
import com.TrackFlow.shipment.dtos.requests.UpdateStatusRequest;
import com.TrackFlow.shipment.dtos.response.ShipmentResponse;
import com.TrackFlow.shipment.entities.Shipment;
import com.TrackFlow.shipment.entities.ShipmentStatusHistory;
import com.TrackFlow.shipment.entities.enums.ShipmentStatus;
import com.TrackFlow.shipment.events.ShipmentCreatedEvent;
import com.TrackFlow.shipment.events.ShipmentStatusChangedEvent;
import com.TrackFlow.shipment.exceptions.exceptons.InvalidStatusTransitionException;
import com.TrackFlow.shipment.exceptions.exceptons.ShipmentNotFoundException;
import com.TrackFlow.shipment.repositories.ShipmentRepository;
import com.TrackFlow.shipment.services.interfaces.ShipmentService;
import com.TrackFlow.shipment.services.interfaces.TrackingNumberGenerator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.UUID;


@Service
public class ShipmentServiceImpl implements ShipmentService {
    private final ShipmentRepository shipmentRepository;
    private final TrackingNumberGenerator trackingNumberGenerator;
    private final ApplicationEventPublisher eventPublisher;

    public ShipmentServiceImpl(
            ShipmentRepository shipmentRepository,
            TrackingNumberGenerator trackingNumberGenerator,
            ApplicationEventPublisher eventPublisher
    ){
        this.shipmentRepository = shipmentRepository;
        this.trackingNumberGenerator = trackingNumberGenerator;
        this.eventPublisher = eventPublisher;
    }
    @Override
    @Transactional
    @Retryable(includes = DataIntegrityViolationException.class, maxRetries = 3)
    @PreAuthorize("hasRole('AGENT')")
    public ShipmentResponse addShipment(CreateShipmentRequest request) {

        //SHIPMENT CREATION
        Shipment shipment = new Shipment();
        shipment.setTrackingNumber(trackingNumberGenerator.generate());
        shipment.setOriginAddress(request.originAddress());
        shipment.setDestinationAddress(request.destinationAddress());
        shipment.setPickupDate(request.pickupDate());
        shipment.setWeightKg(BigDecimal.valueOf(request.weightKg()));
        shipment.setCustomerId(UUID.randomUUID()); // TODO: replace it with customerId from customer_service

         //SHIPMENT STATUS HISTORY CREATION
        ShipmentStatusHistory shipmentStatusHistory = createShipmentStatusHistory(shipment, null);

        shipment.getShipmentStatusHistories().add(shipmentStatusHistory);
        Shipment saved = shipmentRepository.save(shipment);
        eventPublisher.publishEvent(shipmentCreatedEvent(saved));
        return toShipmentResponse(saved);
    }


    @Override
    @PreAuthorize("hasRole('CUSTOMER')")
    public ShipmentResponse getShipmentByTrackingNumberForClient(String trackingNumber) {
        UUID customerId = currentCustomerId();
        Shipment shipment = shipmentRepository.findByTrackingNumberAndCustomerId(trackingNumber, customerId)
                .orElseThrow(() -> new ShipmentNotFoundException("Shipment not found with tracking number: " + trackingNumber));
        return toShipmentResponse(shipment);
    }

    @Override
    @PreAuthorize("hasRole('AGENT')")
    public ShipmentResponse getShipmentByTrackingNumberForAgent(String trackingNumber){
        Shipment shipment = shipmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ShipmentNotFoundException("Shipment not found with tracking number: " + trackingNumber));
        return toShipmentResponse(shipment);
    }





    @Override
    @Transactional
    @PreAuthorize("hasRole('AGENT')")
    public ShipmentResponse updateShipmentStatus(UpdateStatusRequest request) {
        Shipment shipment = shipmentRepository.findByTrackingNumber(request.trackingNumber())
                .orElseThrow(() ->new  ShipmentNotFoundException("Shipment not found with tracking number: " + request.trackingNumber()));

        ShipmentStatus currentStatus = shipment.getStatus();
        ShipmentStatus nextStatus = request.status();
        if (!currentStatus.canTransitionTo(nextStatus)) {
            throw new InvalidStatusTransitionException(
                    "Invalid status transition: " + currentStatus + " -> " +  nextStatus
            );
        }

        shipment.setStatus(nextStatus);
        ShipmentStatusHistory shipmentStatusHistory = createShipmentStatusHistory(shipment, currentStatus);
        shipment.getShipmentStatusHistories().add(shipmentStatusHistory);
        eventPublisher.publishEvent(shipmentStatusChangedEvent(shipment.getTrackingNumber(), shipment.getCustomerId(), currentStatus, nextStatus));
        return toShipmentResponse(shipment);
    }

    private ShipmentResponse toShipmentResponse(Shipment shipment) {
        return new ShipmentResponse(
                shipment.getTrackingNumber(),
                shipment.getOriginAddress(),
                shipment.getDestinationAddress(),
                shipment.getPickupDate(),
                shipment.getEstimatedDeliveryDate(),
                shipment.getStatus(),
                shipment.getWeightKg(),
                shipment.getCreatedAt(),
                shipment.getUpdatedAt()
        );
    }

    private ShipmentStatusHistory createShipmentStatusHistory(Shipment shipment, ShipmentStatus from)
    {
        ShipmentStatusHistory shipmentStatusHistory = new ShipmentStatusHistory();
        shipmentStatusHistory.setFromStatus(from);
        shipmentStatusHistory.setShipment(shipment);
        shipmentStatusHistory.setToStatus(shipment.getStatus());

        return shipmentStatusHistory;
    }

    private UUID currentCustomerId(){
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String claim = jwt.getClaimAsString("customerId");
        if (claim == null) {
            throw new AccessDeniedException("Missing customerId claim");
        }
        return UUID.fromString(claim);
    }

    private ShipmentCreatedEvent shipmentCreatedEvent(Shipment shipment){
        return new ShipmentCreatedEvent(
                UUID.randomUUID(),
                shipment.getTrackingNumber(),
                shipment.getCustomerId(),
                shipment.getOriginAddress(),
                shipment.getDestinationAddress(),
                shipment.getPickupDate(),
                shipment.getWeightKg(),
                shipment.getStatus(),
                Instant.now()
        );
    }

    private ShipmentStatusChangedEvent shipmentStatusChangedEvent(
            String trackingNumber,
            UUID customerId,
            ShipmentStatus previousStatus,
            ShipmentStatus newStatus
    ){
        return new ShipmentStatusChangedEvent(
                UUID.randomUUID(),
                trackingNumber,
                customerId,
                previousStatus,
                newStatus,
                Instant.now()
        );
    }
}
