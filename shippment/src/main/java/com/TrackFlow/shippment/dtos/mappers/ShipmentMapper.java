package com.TrackFlow.shippment.dtos.mappers;

import com.TrackFlow.shippment.dtos.entitiesDTO.ShipmentDTO;
import com.TrackFlow.shippment.dtos.entitiesDTO.ShipmentDTO;
import com.TrackFlow.shippment.entities.Shipment;

public class ShipmentMapper {

    public static ShipmentDTO toDto(Shipment shipment) {
        return new ShipmentDTO(
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

    public static Shipment toEntity(ShipmentDTO dto) {
        Shipment shipment = new Shipment();
        shipment.setTrackingNumber(dto.trackingNumber());
        shipment.setOriginAddress(dto.originAddress());
        shipment.setDestinationAddress(dto.destinationAddress());
        shipment.setPickupDate(dto.pickupDate());
        shipment.setEstimatedDeliveryDate(dto.estimatedDeliveryDate());
        shipment.setStatus(dto.status());
        shipment.setWeightKg(dto.weightKg());
        shipment.setCreatedBy(dto.createdBy());
        shipment.setCreatedAt(dto.createdAt());
        shipment.setUpdatedAt(dto.updatedAt());
        return shipment;
    }
}