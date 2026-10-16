package com.TrackFlow.shippment.services.implementations;

import com.TrackFlow.shippment.dtos.requests.CreateShipmentRequest;
import com.TrackFlow.shippment.dtos.response.ShipmentResponse;
import com.TrackFlow.shippment.entities.Shipment;
import com.TrackFlow.shippment.repositories.ShipmentRepository;
import com.TrackFlow.shippment.services.interfaces.ShipmentService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ShipmentServiceImpl implements ShipmentService {
    private final ShipmentRepository shipmentRepository;

    public ShipmentServiceImpl(ShipmentRepository shipmentRepository){
        this.shipmentRepository = shipmentRepository;
    }

    @Override
    @Retryable(includes = DataIntegrityViolationException.class, maxRetries = 3)
    public ShipmentResponse addShipment(CreateShipmentRequest request) {
        Shipment shipment = new Shipment();
        shipment.setOriginAddress(request.originAddress());
        shipment.setDestinationAddress(request.destinationAddress());
        shipment.setPickupDate(request.pickupDate());
        shipment.setWeightKg(BigDecimal.valueOf(request.weightKg()));

        Shipment saved = shipmentRepository.save(shipment);

        return new ShipmentResponse(
                saved.getTrackingNumber(),
                saved.getOriginAddress(),
                saved.getDestinationAddress(),
                saved.getPickupDate(),
                saved.getEstimatedDeliveryDate(),
                saved.getStatus(),
                saved.getWeightKg(),
                saved.getCreatedBy(),
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }
}
