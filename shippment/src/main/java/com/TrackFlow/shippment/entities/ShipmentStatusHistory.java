package com.TrackFlow.shippment.entities;

import com.TrackFlow.shippment.entities.enums.ShipmentStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Data @AllArgsConstructor
@NoArgsConstructor
public class ShipmentStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Shipment shipment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private ShipmentStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus toStatus;

    private Instant changedAt;


    @PrePersist
    void initializing() {
        changedAt = Instant.now();
    }
}
