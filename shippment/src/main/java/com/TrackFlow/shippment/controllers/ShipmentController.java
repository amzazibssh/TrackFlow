package com.TrackFlow.shippment.controllers;

import com.TrackFlow.shippment.dtos.requests.CreateShipmentRequest;
import com.TrackFlow.shippment.dtos.requests.UpdateStatusRequest;
import com.TrackFlow.shippment.dtos.response.ShipmentResponse;
import com.TrackFlow.shippment.entities.enums.ShipmentStatus;
import com.TrackFlow.shippment.services.interfaces.ShipmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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
        ShipmentResponse response = shipmentService.addShipment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{trackingNumber}")
    public ResponseEntity<ShipmentResponse> getShipmentByTrackingNumber(
            @PathVariable String trackingNumber) {
        ShipmentResponse response = shipmentService.getShipmentByTrackingNumber(trackingNumber);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/status")
    public ResponseEntity<ShipmentResponse> updateShipmentStatus(
            @RequestBody @Valid UpdateStatusRequest request) {

        ShipmentResponse response = shipmentService.updateShipmentStatus(request);
        return ResponseEntity.ok(response);
    }
}
