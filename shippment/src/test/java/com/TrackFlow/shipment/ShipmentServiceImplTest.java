package com.TrackFlow.shipment;

import com.TrackFlow.shipment.dtos.entitiesDTO.CustomerDTO;
import com.TrackFlow.shipment.dtos.requests.CreateShipmentRequest;
import com.TrackFlow.shipment.dtos.requests.UpdateStatusRequest;
import com.TrackFlow.shipment.dtos.response.ShipmentResponse;
import com.TrackFlow.shipment.entities.Shipment;
import com.TrackFlow.shipment.entities.ShipmentStatusHistory;
import com.TrackFlow.shipment.entities.enums.IdType;
import com.TrackFlow.shipment.entities.enums.ShipmentStatus;
import com.TrackFlow.shipment.exceptions.exceptons.InvalidStatusTransitionException;
import com.TrackFlow.shipment.exceptions.exceptons.ShipmentNotFoundException;
import com.TrackFlow.shipment.repositories.ShipmentRepository;
import com.TrackFlow.shipment.services.implementations.ShipmentServiceImpl;
import com.TrackFlow.shipment.services.interfaces.TrackingNumberGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ShipmentServiceImplTest {

    @Mock
    ShipmentRepository shipmentRepository;

    @Mock
    TrackingNumberGenerator trackingNumberGenerator;

    @InjectMocks
    ShipmentServiceImpl shipmentService;

    private Shipment shipment;
    private UUID customerId;

    @BeforeEach
    void setup() {
        customerId = UUID.randomUUID();
        shipment = new Shipment();
        shipment.setTrackingNumber("TF-ABCD123456");
        shipment.setOriginAddress("Marrakech");
        shipment.setDestinationAddress("Casablanca");
        shipment.setPickupDate(LocalDate.of(2026, 10, 25));
        shipment.setWeightKg(BigDecimal.valueOf(2.5));
        shipment.setCustomerId(customerId);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private CreateShipmentRequest createRequest() {
        return new CreateShipmentRequest(
                "Marrakech", "Casablanca", LocalDate.of(2026, 10, 25), 2.5,
                new CustomerDTO(IdType.CIN, "EE989898", "test_name", "test_last_name",
                        "0607080900", "test@gmail.com")
        );
    }

    private void authenticateWithClaims(String claimName, String claimValue) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim(claimName, claimValue)
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    // ---------- addShipment ----------

    @Test
    void shouldSaveShipmentWithRequestDataWhenAddShipment() {
        when(trackingNumberGenerator.generate()).thenReturn("TF-TEST000001");
        when(shipmentRepository.save(any(Shipment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShipmentResponse response = shipmentService.addShipment(createRequest());

        ArgumentCaptor<Shipment> captor = ArgumentCaptor.forClass(Shipment.class);
        verify(shipmentRepository).save(captor.capture());
        Shipment saved = captor.getValue();

        assertEquals("TF-TEST000001", saved.getTrackingNumber());
        assertEquals("Marrakech", saved.getOriginAddress());
        assertEquals("Casablanca", saved.getDestinationAddress());
        assertEquals(LocalDate.of(2026, 10, 25), saved.getPickupDate());
        assertEquals(BigDecimal.valueOf(2.5), saved.getWeightKg());
        assertNotNull(saved.getCustomerId());
        assertNotNull(response);
        assertEquals("TF-TEST000001", response.trackingNumber());
    }

    @Test
    void shouldCreateInitialStatusHistoryWhenAddShipment() {
        when(trackingNumberGenerator.generate()).thenReturn("TF-TEST000001");
        when(shipmentRepository.save(any(Shipment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        shipmentService.addShipment(createRequest());

        ArgumentCaptor<Shipment> captor = ArgumentCaptor.forClass(Shipment.class);
        verify(shipmentRepository).save(captor.capture());
        Shipment saved = captor.getValue();

        assertEquals(1, saved.getShipmentStatusHistories().size());
        ShipmentStatusHistory history = saved.getShipmentStatusHistories()
                .stream().findFirst().orElseThrow();
        assertNull(history.getFromStatus());
        assertEquals(saved.getStatus(), history.getToStatus());
        assertSame(saved, history.getShipment());
    }

    // ---------- getShipmentByTrackingNumberForAgent ----------

    @Test
    void shouldReturnShipmentForAgentWhenTrackingNumberExists() {
        when(shipmentRepository.findByTrackingNumber(shipment.getTrackingNumber()))
                .thenReturn(Optional.of(shipment));

        ShipmentResponse response =
                shipmentService.getShipmentByTrackingNumberForAgent(shipment.getTrackingNumber());

        assertEquals(shipment.getTrackingNumber(), response.trackingNumber());
        assertEquals("Marrakech", response.originAddress());
        assertEquals("Casablanca", response.destinationAddress());
    }

    @Test
    void shouldThrowForAgentWhenTrackingNumberDoesNotExist() {
        String trackingNumber = "TF-UNKNOWN";
        when(shipmentRepository.findByTrackingNumber(trackingNumber))
                .thenReturn(Optional.empty());

        ShipmentNotFoundException exception = assertThrows(
                ShipmentNotFoundException.class,
                () -> shipmentService.getShipmentByTrackingNumberForAgent(trackingNumber)
        );
        assertEquals("Shipment not found with tracking number: " + trackingNumber, exception.getMessage());
    }

    // ---------- getShipmentByTrackingNumberForClient ----------

    @Test
    void shouldReturnShipmentForClientUsingCustomerIdFromToken() {
        authenticateWithClaims("customerId", customerId.toString());
        when(shipmentRepository.findByTrackingNumberAndCustomerId(shipment.getTrackingNumber(), customerId))
                .thenReturn(Optional.of(shipment));

        ShipmentResponse response =
                shipmentService.getShipmentByTrackingNumberForClient(shipment.getTrackingNumber());

        assertEquals(shipment.getTrackingNumber(), response.trackingNumber());
        verify(shipmentRepository).findByTrackingNumberAndCustomerId(shipment.getTrackingNumber(), customerId);
    }

    @Test
    void shouldThrowForClientWhenShipmentIsNotFoundForThisCustomer() {
        authenticateWithClaims("customerId", customerId.toString());
        when(shipmentRepository.findByTrackingNumberAndCustomerId(shipment.getTrackingNumber(), customerId))
                .thenReturn(Optional.empty());

        assertThrows(ShipmentNotFoundException.class,
                () -> shipmentService.getShipmentByTrackingNumberForClient(shipment.getTrackingNumber()));

        verify(shipmentRepository, never()).findByTrackingNumber(any());
    }

    @Test
    void shouldThrowAccessDeniedForClientWhenCustomerIdClaimIsMissing() {
        authenticateWithClaims("sub", "agent1");

        assertThrows(AccessDeniedException.class,
                () -> shipmentService.getShipmentByTrackingNumberForClient(shipment.getTrackingNumber()));

        verify(shipmentRepository, never()).findByTrackingNumberAndCustomerId(any(), any());
    }

    // ---------- updateShipmentStatus ----------

    @Test
    void shouldThrowExceptionWhenShipmentDoesNotExist() {
        when(shipmentRepository.findByTrackingNumber(shipment.getTrackingNumber()))
                .thenReturn(Optional.empty());
        UpdateStatusRequest request =
                new UpdateStatusRequest(shipment.getTrackingNumber(), ShipmentStatus.PICKED_UP);

        Exception exception = assertThrows(
                ShipmentNotFoundException.class,
                () -> shipmentService.updateShipmentStatus(request)
        );
        assertEquals("Shipment not found with tracking number: " + request.trackingNumber(),
                exception.getMessage());
    }

    @Test
    void shouldInvokeFindByTrackingNumberWhenUpdateShipmentStatusIsCalled() {
        UpdateStatusRequest request =
                new UpdateStatusRequest(shipment.getTrackingNumber(), ShipmentStatus.CANCELED);
        when(shipmentRepository.findByTrackingNumber(shipment.getTrackingNumber()))
                .thenReturn(Optional.of(shipment));

        shipmentService.updateShipmentStatus(request);

        verify(shipmentRepository).findByTrackingNumber(shipment.getTrackingNumber());
    }

    @Test
    void shouldThrowExceptionWhenStatusConversionIsNotAllowed() {
        shipment.setStatus(ShipmentStatus.CANCELED);
        when(shipmentRepository.findByTrackingNumber(shipment.getTrackingNumber()))
                .thenReturn(Optional.of(shipment));
        UpdateStatusRequest request =
                new UpdateStatusRequest(shipment.getTrackingNumber(), ShipmentStatus.CREATED);

        InvalidStatusTransitionException exception = assertThrows(
                InvalidStatusTransitionException.class,
                () -> shipmentService.updateShipmentStatus(request)
        );
        assertEquals("Invalid status transition: CANCELED -> CREATED", exception.getMessage());
    }

    @Test
    void shouldUpdateShipmentStatusWhenTransitionIsValid() {
        UpdateStatusRequest request =
                new UpdateStatusRequest(shipment.getTrackingNumber(), ShipmentStatus.PICKED_UP);
        when(shipmentRepository.findByTrackingNumber(shipment.getTrackingNumber()))
                .thenReturn(Optional.of(shipment));
        int sizeBefore = shipment.getShipmentStatusHistories().size();

        shipmentService.updateShipmentStatus(request);

        assertEquals(request.status(), shipment.getStatus());
        assertEquals(sizeBefore + 1, shipment.getShipmentStatusHistories().size());
    }

    @Test
    void shouldRecordFromAndToStatusInHistoryWhenTransitionIsValid() {
        ShipmentStatus previous = shipment.getStatus();
        UpdateStatusRequest request =
                new UpdateStatusRequest(shipment.getTrackingNumber(), ShipmentStatus.PICKED_UP);
        when(shipmentRepository.findByTrackingNumber(shipment.getTrackingNumber()))
                .thenReturn(Optional.of(shipment));

        shipmentService.updateShipmentStatus(request);

        ShipmentStatusHistory history = shipment.getShipmentStatusHistories()
                .stream().findFirst().orElseThrow();
        assertEquals(previous, history.getFromStatus());
        assertEquals(ShipmentStatus.PICKED_UP, history.getToStatus());
        assertSame(shipment, history.getShipment());
    }

    @Test
    void shouldNotAddHistoryWhenTransitionIsInvalid() {
        shipment.setStatus(ShipmentStatus.CANCELED);
        when(shipmentRepository.findByTrackingNumber(shipment.getTrackingNumber()))
                .thenReturn(Optional.of(shipment));
        UpdateStatusRequest request =
                new UpdateStatusRequest(shipment.getTrackingNumber(), ShipmentStatus.CREATED);

        assertThrows(InvalidStatusTransitionException.class,
                () -> shipmentService.updateShipmentStatus(request));

        assertEquals(ShipmentStatus.CANCELED, shipment.getStatus());
        assertTrue(shipment.getShipmentStatusHistories().isEmpty());
    }

    @Test
    void shouldNotCallSaveWhenUpdateShipmentStatus() {
        UpdateStatusRequest request =
                new UpdateStatusRequest(shipment.getTrackingNumber(), ShipmentStatus.PICKED_UP);
        when(shipmentRepository.findByTrackingNumber(shipment.getTrackingNumber()))
                .thenReturn(Optional.of(shipment));

        shipmentService.updateShipmentStatus(request);

        verify(shipmentRepository, never()).save(any());
    }
}