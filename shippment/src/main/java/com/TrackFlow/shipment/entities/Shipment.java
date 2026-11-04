package com.TrackFlow.shipment.entities;

import com.TrackFlow.shipment.entities.enums.ShipmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Shipment {
    @Id
    @Column(name = "tracking_number", length = 20, updatable = false)
    private String trackingNumber;

    private String originAddress;

    private String destinationAddress;
    private LocalDate pickupDate;
    private LocalDate estimatedDeliveryDate; // TODO: replace the fixed +15 days with a dedicated estimation service
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus status = ShipmentStatus.CREATED;
    private BigDecimal weightKg;

    @OneToMany(mappedBy = "shipment", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude // to avoid infinite recursion
    private List<ShipmentStatusHistory> shipmentStatusHistories = new ArrayList<>();
    private String createdBy; // TODO: replace "user" with a user id when identity-service exists
    private Instant createdAt, updatedAt; // TODO: add @PreUpdate to refresh updatedAt
    @Version
    private Long version; // TODO: optimistic locking, handle OptimisticLockException
    @Column(name = "customer_id", nullable = false)
    private UUID customerId; // TODO: retreive the client id from customer service

    @PrePersist
    void initializing() {
        estimatedDeliveryDate = LocalDate.now().plusDays(15);
        createdBy = "user";
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}