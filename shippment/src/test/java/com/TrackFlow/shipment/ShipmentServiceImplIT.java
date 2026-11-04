package com.TrackFlow.shipment;

import com.TrackFlow.shipment.dtos.entitiesDTO.CustomerDTO;
import com.TrackFlow.shipment.dtos.requests.CreateShipmentRequest;
import com.TrackFlow.shipment.dtos.requests.UpdateStatusRequest;
import com.TrackFlow.shipment.dtos.response.ShipmentResponse;
import com.TrackFlow.shipment.entities.Shipment;
import com.TrackFlow.shipment.entities.enums.IdType;
import com.TrackFlow.shipment.entities.enums.ShipmentStatus;
import com.TrackFlow.shipment.exceptions.exceptons.InvalidStatusTransitionException;
import com.TrackFlow.shipment.exceptions.exceptons.ShipmentNotFoundException;
import com.TrackFlow.shipment.repositories.ShipmentRepository;
import com.TrackFlow.shipment.services.interfaces.ShipmentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
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

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void authenticateAsAgent() {
        authenticate(null, "AGENT");
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        shipmentRepository.deleteAll();
    }

    private void authenticate(UUID customerId, String role) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "test-user");
        if (customerId != null) {
            builder.claim("customerId", customerId.toString());
        }
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(builder.build(),
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private CreateShipmentRequest createRequest() {
        return new CreateShipmentRequest(
                "Marrakech", "Casablanca", LocalDate.of(2026, 10, 25), 2.5,
                new CustomerDTO(IdType.CIN, "EE989898", "test_name", "test_last_name",
                        "0607080900", "test@gmail.com")
        );
    }

    private ShipmentResponse createShipment() {
        return shipmentService.addShipment(createRequest());
    }

    // ---------- addShipment ----------

    @Test
    void shouldPersistShipmentWhenAddShipment() {
        ShipmentResponse response = createShipment();

        assertNotNull(response.trackingNumber());
        Shipment found = shipmentRepository.findByTrackingNumber(response.trackingNumber())
                .orElseThrow();
        assertEquals("Marrakech", found.getOriginAddress());
        assertEquals("Casablanca", found.getDestinationAddress());
        assertEquals(LocalDate.of(2026, 10, 25), found.getPickupDate());
        assertNotNull(found.getCustomerId());
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
    void shouldDenyAddShipmentWhenCallerIsCustomer() {
        authenticate(UUID.randomUUID(), "CUSTOMER");

        assertThrows(AccessDeniedException.class, this::createShipment);
    }

    // ---------- getShipmentByTrackingNumberForAgent ----------

    @Test
    void shouldReturnShipmentForAgentWhenTrackingNumberExists() {
        ShipmentResponse created = createShipment();

        ShipmentResponse found =
                shipmentService.getShipmentByTrackingNumberForAgent(created.trackingNumber());

        assertEquals(created.trackingNumber(), found.trackingNumber());
        assertEquals("Marrakech", found.originAddress());
    }

    @Test
    void shouldThrowForAgentWhenTrackingNumberDoesNotExist() {
        assertThrows(ShipmentNotFoundException.class,
                () -> shipmentService.getShipmentByTrackingNumberForAgent("TF-UNKNOWN"));
    }

    // ---------- getShipmentByTrackingNumberForClient ----------

    @Test
    void shouldReturnShipmentForClientWhenCustomerOwnsIt() {
        ShipmentResponse created = createShipment();
        UUID ownerId = shipmentRepository.findByTrackingNumber(created.trackingNumber())
                .orElseThrow().getCustomerId();
        authenticate(ownerId, "CUSTOMER");

        ShipmentResponse found =
                shipmentService.getShipmentByTrackingNumberForClient(created.trackingNumber());

        assertEquals(created.trackingNumber(), found.trackingNumber());
    }

    @Test
    void shouldThrowForClientWhenShipmentBelongsToAnotherCustomer() {
        ShipmentResponse created = createShipment();
        authenticate(UUID.randomUUID(), "CUSTOMER");

        assertThrows(ShipmentNotFoundException.class,
                () -> shipmentService.getShipmentByTrackingNumberForClient(created.trackingNumber()));
    }

    @Test
    void shouldDenyClientMethodWhenCallerIsAgent() {
        ShipmentResponse created = createShipment();

        assertThrows(AccessDeniedException.class,
                () -> shipmentService.getShipmentByTrackingNumberForClient(created.trackingNumber()));
    }

    // ---------- updateShipmentStatus ----------

    @Test
    void shouldPersistNewStatusAndHistoryWhenTransitionIsValid() {
        String trackingNumber = createShipment().trackingNumber();

        shipmentService.updateShipmentStatus(new UpdateStatusRequest(trackingNumber, ShipmentStatus.PICKED_UP));

        transactionTemplate.executeWithoutResult(status -> {
            Shipment found = shipmentRepository.findByTrackingNumber(trackingNumber).orElseThrow();
            assertEquals(ShipmentStatus.PICKED_UP, found.getStatus());
            assertEquals(2, found.getShipmentStatusHistories().size());
        });
    }

    @Test
    void shouldNotPersistChangesWhenTransitionIsInvalid() {
        String trackingNumber = createShipment().trackingNumber();
        shipmentService.updateShipmentStatus(new UpdateStatusRequest(trackingNumber, ShipmentStatus.CANCELED));

        assertThrows(InvalidStatusTransitionException.class,
                () -> shipmentService.updateShipmentStatus(
                        new UpdateStatusRequest(trackingNumber, ShipmentStatus.CREATED)));

        transactionTemplate.executeWithoutResult(status -> {
            Shipment found = shipmentRepository.findById(trackingNumber).orElseThrow();
            assertEquals(ShipmentStatus.CANCELED, found.getStatus());
            assertEquals(2, found.getShipmentStatusHistories().size());
        });
    }

    @Test
    void shouldDenyUpdateShipmentStatusWhenCallerIsCustomer() {
        String trackingNumber = createShipment().trackingNumber();
        authenticate(UUID.randomUUID(), "CUSTOMER");

        assertThrows(AccessDeniedException.class,
                () -> shipmentService.updateShipmentStatus(
                        new UpdateStatusRequest(trackingNumber, ShipmentStatus.PICKED_UP)));
    }
}