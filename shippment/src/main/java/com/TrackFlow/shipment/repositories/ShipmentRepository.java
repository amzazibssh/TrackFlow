package com.TrackFlow.shipment.repositories;

import com.TrackFlow.shipment.entities.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, String> {
    Optional<Shipment> findByTrackingNumber(String trackingNumber);
    Optional<Shipment> findByTrackingNumberAndCustomerId(String trackingNumber, UUID customerId);
}
