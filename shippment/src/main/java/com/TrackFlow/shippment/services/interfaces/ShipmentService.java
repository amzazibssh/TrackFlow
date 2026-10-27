package com.TrackFlow.shippment.services.interfaces;

import com.TrackFlow.shippment.dtos.requests.CreateShipmentRequest;
import com.TrackFlow.shippment.dtos.requests.UpdateStatusRequest;
import com.TrackFlow.shippment.dtos.response.ShipmentResponse;


public interface ShipmentService {
    ShipmentResponse addShipment(CreateShipmentRequest request);
    ShipmentResponse getShipmentByTrackingNumber(String trackingNumber);
    ShipmentResponse updateShipmentStatus(UpdateStatusRequest request);
}
