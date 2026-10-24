package com.TrackFlow.shippment.repositories;

import com.TrackFlow.shippment.entities.ShipmentStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ShipmentStatusHistoryRepository extends JpaRepository<ShipmentStatusHistory, UUID> {
}
