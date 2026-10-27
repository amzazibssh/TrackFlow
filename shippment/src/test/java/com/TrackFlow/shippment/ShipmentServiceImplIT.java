package com.TrackFlow.shippment;

import com.TrackFlow.shippment.dtos.requests.CreateShipmentRequest;
import com.TrackFlow.shippment.dtos.requests.UpdateStatusRequest;
import com.TrackFlow.shippment.dtos.response.ShipmentResponse;
import com.TrackFlow.shippment.entities.Shipment;
import com.TrackFlow.shippment.entities.enums.ShipmentStatus;
import com.TrackFlow.shippment.exceptions.exceptons.InvalidStatusTransitionException;
import com.TrackFlow.shippment.exceptions.exceptons.ShipmentNotFoundException;
import com.TrackFlow.shippment.repositories.ShipmentRepository;
import com.TrackFlow.shippment.services.interfaces.ShipmentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class ShipmentServiceImplIT {

    @Autowired
    private ShipmentService shipmentService;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @AfterEach
    void cleanUp() {
        shipmentRepository.deleteAll();
    }

    private ShipmentResponse createShipment() {
        return shipmentService.addShipment(new CreateShipmentRequest(
                "Marrakech", "Casablanca", LocalDate.of(2026, 10, 25), 2.5
        ));
    }

    @Test
    void shouldPersistShipmentWhenAddShipment() {
        ShipmentResponse response = createShipment();

        assertNotNull(response.trackingNumber());
        Shipment found = shipmentRepository.findByTrackingNumber(response.trackingNumber())
                .orElseThrow();
        assertEquals("Marrakech", found.getOriginAddress());
        assertEquals("Casablanca", found.getDestinationAddress());
        assertEquals(LocalDate.of(2026, 10, 25), found.getPickupDate());
    }

    @Test
    void shouldPersistInitialStatusHistoryWhenAddShipment() {
        ShipmentResponse response = createShipment();

        transactionTemplate.executeWithoutResult(status -> {
            Shipment found = shipmentRepository.findByTrackingNumber(response.trackingNumber())
                    .orElseThrow();
            assertEquals(1, found.getShipmentStatusHistories().size());
            assertNull(found.getShipmentStatusHistories().iterator().next().getFromStatus());
        });
    }

    @Test
    void shouldReturnShipmentWhenTrackingNumberExists() {
        ShipmentResponse created = createShipment();

        ShipmentResponse found = shipmentService.getShipmentByTrackingNumber(created.trackingNumber());

        assertEquals(created.trackingNumber(), found.trackingNumber());
        assertEquals("Marrakech", found.originAddress());
    }

    @Test
    void shouldThrowWhenTrackingNumberDoesNotExist() {
        assertThrows(ShipmentNotFoundException.class,
                () -> shipmentService.getShipmentByTrackingNumber("TF-UNKNOWN"));
    }

    @Test
    void shouldPersistNewStatusAndHistoryWhenTransitionIsValid() {
        ShipmentResponse created = createShipment();
        UUID id = shipmentRepository.findByTrackingNumber(created.trackingNumber())
                .orElseThrow().getId();

        shipmentService.updateShipmentStatus(new UpdateStatusRequest(id, ShipmentStatus.PICKED_UP));

        transactionTemplate.executeWithoutResult(status -> {
            Shipment found = shipmentRepository.findById(id).orElseThrow();
            assertEquals(ShipmentStatus.PICKED_UP, found.getStatus());
            assertEquals(2, found.getShipmentStatusHistories().size());
        });
    }

    @Test
    void shouldNotPersistChangesWhenTransitionIsInvalid() {
        ShipmentResponse created = createShipment();
        UUID id = shipmentRepository.findByTrackingNumber(created.trackingNumber())
                .orElseThrow().getId();
        shipmentService.updateShipmentStatus(new UpdateStatusRequest(id, ShipmentStatus.CANCELED));

        assertThrows(InvalidStatusTransitionException.class,
                () -> shipmentService.updateShipmentStatus(
                        new UpdateStatusRequest(id, ShipmentStatus.CREATED)));

        transactionTemplate.executeWithoutResult(status -> {
            Shipment found = shipmentRepository.findById(id).orElseThrow();
            assertEquals(ShipmentStatus.CANCELED, found.getStatus());
            assertEquals(2, found.getShipmentStatusHistories().size());
        });
    }

    @Test
    void shouldThrowWhenUpdatingUnknownShipment() {
        assertThrows(ShipmentNotFoundException.class,
                () -> shipmentService.updateShipmentStatus(
                        new UpdateStatusRequest(UUID.randomUUID(), ShipmentStatus.PICKED_UP)));
    }
}
