package com.TrackFlow.shippment.controllers;

import com.TrackFlow.shippment.dtos.requests.CreateShipmentRequest;
import com.TrackFlow.shippment.dtos.response.ShipmentResponse;
import com.TrackFlow.shippment.services.interfaces.ShipmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/api/shipment")
public class ShipmentController {

    private ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService){
        this.shipmentService = shipmentService;
    }

    // api to create a shipment object and persist it in oracle database
    @PostMapping
    public ResponseEntity<ShipmentResponse> addShipment(@Valid @RequestBody CreateShipmentRequest request){
        // create service return response entity
        ShipmentResponse response = shipmentService.addShipment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
