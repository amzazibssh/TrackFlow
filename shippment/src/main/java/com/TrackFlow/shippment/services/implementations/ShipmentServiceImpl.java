package com.TrackFlow.shippment.services.implementations;

import com.TrackFlow.shippment.dtos.requests.CreateShipmentRequest;
import com.TrackFlow.shippment.dtos.requests.UpdateStatusRequest;
import com.TrackFlow.shippment.dtos.response.ShipmentResponse;
import com.TrackFlow.shippment.entities.Shipment;
import com.TrackFlow.shippment.entities.ShipmentStatusHistory;
import com.TrackFlow.shippment.entities.enums.ShipmentStatus;
import com.TrackFlow.shippment.exceptions.exceptons.InvalidStatusTransitionException;
import com.TrackFlow.shippment.exceptions.exceptons.ShipmentNotFoundException;
import com.TrackFlow.shippment.repositories.ShipmentRepository;
import com.TrackFlow.shippment.services.interfaces.ShipmentService;
import com.TrackFlow.shippment.services.interfaces.TrackingNumberGenerator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;



@Service
public class ShipmentServiceImpl implements ShipmentService {
    private final ShipmentRepository shipmentRepository;
    private final TrackingNumberGenerator trackingNumberGenerator;

    public ShipmentServiceImpl(
            ShipmentRepository shipmentRepository,
            TrackingNumberGenerator trackingNumberGenerator
    ){
        this.shipmentRepository = shipmentRepository;
        this.trackingNumberGenerator = trackingNumberGenerator;
    }
    @Override
    @Transactional
    @Retryable(includes = DataIntegrityViolationException.class, maxRetries = 3)
    public ShipmentResponse addShipment(CreateShipmentRequest request) {
        //SHIPMENT CREATION
        Shipment shipment = new Shipment();
        shipment.setOriginAddress(request.originAddress());
        shipment.setTrackingNumber(trackingNumberGenerator.generate());
        shipment.setDestinationAddress(request.destinationAddress());
        shipment.setPickupDate(request.pickupDate());
        shipment.setWeightKg(BigDecimal.valueOf(request.weightKg()));
         //SHIPMENT STATUS HISTORY CREATION
        ShipmentStatusHistory shipmentStatusHistory = createShipmentStatusHistory(shipment, null);

        shipment.getShipmentStatusHistories().add(shipmentStatusHistory);
        Shipment saved = shipmentRepository.save(shipment);
        return toShipmentResponse(saved);
    }


    @Override
    public ShipmentResponse getShipmentByTrackingNumber(String trackingNumber) {
        Shipment shipment = shipmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ShipmentNotFoundException("Shipment not found with tracking number: " + trackingNumber));
        return toShipmentResponse(shipment);
    }


    @Override
    @Transactional // transactionel utiiser ici pour assurer la validation du requete sans utiliser save
    public ShipmentResponse updateShipmentStatus(UpdateStatusRequest request) {
        // on charge trouve le shipment  on utilisant l id
        Shipment shipment = shipmentRepository.findById(request.id())
                .orElseThrow(() ->new  ShipmentNotFoundException("Shipment not found with id: " + request.id()));
        // le context est manager par context manager donc une changement dans le status
        // est detecter par le dirty checking mechanisme qui sert a modifier le champs
        // dans la table ici c est status
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
                shipment.getCreatedBy(),
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
}
