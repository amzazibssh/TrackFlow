package com.TrackFlow.shippment.services.interfaces;

import com.TrackFlow.shippment.dtos.requests.CreateShipmentRequest;
import com.TrackFlow.shippment.dtos.response.ShipmentResponse;

public interface ShipmentService {
    ShipmentResponse addShipment(CreateShipmentRequest request);
}
