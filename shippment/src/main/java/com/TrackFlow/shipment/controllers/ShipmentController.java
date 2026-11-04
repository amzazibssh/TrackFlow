package com.TrackFlow.shipment.controllers;

import com.TrackFlow.shipment.dtos.requests.CreateShipmentRequest;
import com.TrackFlow.shipment.dtos.requests.UpdateStatusRequest;
import com.TrackFlow.shipment.dtos.response.ShipmentResponse;
import com.TrackFlow.shipment.services.interfaces.ShipmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/api/shipment")
public class ShipmentController {

    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService){
        this.shipmentService = shipmentService;
    }

    // TODO: restrict to AGENT role
    @PostMapping("/agent")
    public ResponseEntity<ShipmentResponse> addShipment(@Valid @RequestBody CreateShipmentRequest request){
        ShipmentResponse response = shipmentService.addShipment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/client/{trackingNumber}")
    public ResponseEntity<ShipmentResponse> getShipmentByTrackingNumberForClient(
            @PathVariable String trackingNumber) {
        ShipmentResponse response = shipmentService.getShipmentByTrackingNumberForClient(trackingNumber);
        return ResponseEntity.ok(response);
    }

    // TODO: restrict to AGENT role
    @GetMapping("/agent/{trackingNumber}")
    public ResponseEntity<ShipmentResponse> getShipmentByTrackingNumberForAgent(
            @PathVariable String trackingNumber) {
        ShipmentResponse response = shipmentService.getShipmentByTrackingNumberForAgent(trackingNumber);
        return ResponseEntity.ok(response);
    }

    // TODO: restrict to AGENT role
    @PatchMapping("/agent/updateStatus")
    public ResponseEntity<ShipmentResponse> updateShipmentStatus(
            @RequestBody @Valid UpdateStatusRequest request) {

        ShipmentResponse response = shipmentService.updateShipmentStatus(request);
        return ResponseEntity.ok(response);
    }
}
