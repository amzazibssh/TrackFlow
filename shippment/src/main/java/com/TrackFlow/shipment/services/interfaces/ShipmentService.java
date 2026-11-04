package com.TrackFlow.shipment.services.interfaces;

import com.TrackFlow.shipment.dtos.requests.CreateShipmentRequest;
import com.TrackFlow.shipment.dtos.requests.UpdateStatusRequest;
import com.TrackFlow.shipment.dtos.response.ShipmentResponse;

import java.util.UUID;


public interface ShipmentService {
    ShipmentResponse addShipment(CreateShipmentRequest request);
    ShipmentResponse getShipmentByTrackingNumberForClient(String trackingNumber);
    ShipmentResponse updateShipmentStatus(UpdateStatusRequest request);

    ShipmentResponse getShipmentByTrackingNumberForAgent(String trackingNumber);
}
