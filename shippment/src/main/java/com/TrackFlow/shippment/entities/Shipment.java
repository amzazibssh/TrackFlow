package com.TrackFlow.shippment.entities;

import com.TrackFlow.shippment.entities.enums.ShipmentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.resilience.annotation.Retryable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Shipment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true)
    private String trackingNumber; // TODO: handle DataIntegrityViolationException on collision (retry or longer suffix)

    private String originAddress;

    private String destinationAddress;
    private LocalDate pickupDate;
    private LocalDate estimatedDeliveryDate; // TODO: replace the fixed +15 days with a dedicated estimation service
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus status = ShipmentStatus.CREATED; // TODO: enforce canTransitionTo when status updates are implemented
    private BigDecimal weightKg;

    @OneToMany(mappedBy = "shipment", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ShipmentStatusHistory> shipmentStatusHistories = new ArrayList<>();
    private String createdBy; // TODO: replace "user" with a user id when identity-service exists
    private Instant createdAt, updatedAt; // TODO: add @PreUpdate to refresh updatedAt
    @Version
    private Long version; // TODO: optimistic locking, handle OptimisticLockException

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