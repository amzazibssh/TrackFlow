package com.TrackFlow.shipment.repositories;

import com.TrackFlow.shipment.entities.ShipmentStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ShipmentStatusHistoryRepository extends JpaRepository<ShipmentStatusHistory, UUID> {
}
