package com.TrackFlow.shipment;

import com.TrackFlow.shipment.dtos.entitiesDTO.CustomerDTO;
import com.TrackFlow.shipment.dtos.requests.CreateShipmentRequest;
import com.TrackFlow.shipment.dtos.response.ShipmentResponse;
import com.TrackFlow.shipment.entities.Shipment;
import com.TrackFlow.shipment.entities.enums.IdType;
import com.TrackFlow.shipment.repositories.ShipmentRepository;
import com.TrackFlow.shipment.services.interfaces.ShipmentService;
import com.TrackFlow.shipment.services.interfaces.TrackingNumberGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void authenticateAsAgent() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "test-agent")
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_AGENT"))));
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
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
        existing.setCustomerId(UUID.randomUUID());
        shipmentRepository.saveAndFlush(existing);

        when(trackingNumberGenerator.generate()).thenReturn("TF-DUP", "TF-NEW");

        ShipmentResponse response = shipmentService.addShipment(new CreateShipmentRequest(
                "Marrakech", "Casablanca", LocalDate.of(2026, 10, 25), 2.5,
                new CustomerDTO(IdType.CIN, "EE989898", "test_name", "test_last_name",
                        "0607080900", "test@gmail.com")
        ));

        assertEquals("TF-NEW", response.trackingNumber());
        verify(trackingNumberGenerator, times(2)).generate();
        assertTrue(shipmentRepository.findByTrackingNumber("TF-NEW").isPresent());
    }
}