package com.TrackFlow.shipment.exceptions.exceptons;

public class ShipmentNotFoundException extends RuntimeException {
    public ShipmentNotFoundException(String message) {
        super(message);
    }
}
