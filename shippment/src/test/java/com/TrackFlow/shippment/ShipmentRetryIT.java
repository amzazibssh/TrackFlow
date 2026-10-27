package com.TrackFlow.shippment;

import com.TrackFlow.shippment.dtos.requests.CreateShipmentRequest;
import com.TrackFlow.shippment.dtos.response.ShipmentResponse;
import com.TrackFlow.shippment.entities.Shipment;
import com.TrackFlow.shippment.repositories.ShipmentRepository;
import com.TrackFlow.shippment.services.interfaces.ShipmentService;
import com.TrackFlow.shippment.services.interfaces.TrackingNumberGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class ShipmentRetryIT {

    @Autowired
    private ShipmentService shipmentService;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @MockitoBean
    private TrackingNumberGenerator trackingNumberGenerator;

    @AfterEach
    void cleanUp() {
        shipmentRepository.deleteAll();
    }

    @Test
    void shouldRetryWhenTrackingNumberCollides() {
        Shipment existing = new Shipment();
        existing.setTrackingNumber("TF-DUP");
        existing.setOriginAddress("Rabat");
        existing.setDestinationAddress("Fes");
        existing.setPickupDate(LocalDate.of(2026, 10, 25));
        existing.setWeightKg(BigDecimal.valueOf(1));
        shipmentRepository.saveAndFlush(existing);

        when(trackingNumberGenerator.generate()).thenReturn("TF-DUP", "TF-NEW");

        ShipmentResponse response = shipmentService.addShipment(new CreateShipmentRequest(
                "Marrakech", "Casablanca", LocalDate.of(2026, 10, 25), 2.5
        ));

        assertEquals("TF-NEW", response.trackingNumber());
        verify(trackingNumberGenerator, times(2)).generate();
        assertTrue(shipmentRepository.findByTrackingNumber("TF-NEW").isPresent());
    }
}