package com.TrackFlow.shippment;

import com.TrackFlow.shippment.dtos.requests.UpdateStatusRequest;
import com.TrackFlow.shippment.entities.Shipment;
import com.TrackFlow.shippment.entities.ShipmentStatusHistory;
import com.TrackFlow.shippment.entities.enums.ShipmentStatus;
import com.TrackFlow.shippment.exceptions.exceptons.InvalidStatusTransitionException;
import com.TrackFlow.shippment.exceptions.exceptons.ShipmentNotFoundException;
import com.TrackFlow.shippment.repositories.ShipmentRepository;
import com.TrackFlow.shippment.services.implementations.ShipmentServiceImpl;
import com.TrackFlow.shippment.dtos.requests.CreateShipmentRequest;
import com.TrackFlow.shippment.dtos.response.ShipmentResponse;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ShipmentServiceImplTest {

    private Shipment shipment;

    @BeforeEach
    void setup() {
        UUID id = UUID.randomUUID();
        shipment = new Shipment();
        shipment.setId(id);
        shipment.setTrackingNumber("TF-20261024-ABCD1234");
        shipment.setOriginAddress("Marrakech");
        shipment.setDestinationAddress("Casablanca");
        shipment.setPickupDate(LocalDate.of(2026, 10, 25));
        shipment.setWeightKg(BigDecimal.valueOf(2.5));
    }
    @Mock
    ShipmentRepository shipmentRepository;

    @InjectMocks
    ShipmentServiceImpl shipmentService;


    @Test
    void shouldThrowExceptionWhenShipmentDoesNotExist(){

        when(shipmentRepository.findById(shipment.getId()))
                .thenReturn(Optional.empty());
        UpdateStatusRequest request = new UpdateStatusRequest(shipment.getId(), ShipmentStatus.CREATED);
        Exception exception = assertThrows(
                ShipmentNotFoundException.class,
                () -> shipmentService.updateShipmentStatus(request)
        );
        assertEquals("Shipment not found with id: " + request.id(), exception.getMessage());
    }


    @Test
    void shouldInvokeFindByIdWhenUpdateShipmentStatusIsCalled(){
        UpdateStatusRequest request = new UpdateStatusRequest(
                shipment.getId(),
                ShipmentStatus.CANCELED
        );
        when(shipmentRepository.findById(shipment.getId()))
                .thenReturn(Optional.of(shipment));
        shipmentService.updateShipmentStatus(request);
        verify(shipmentRepository).findById(shipment.getId());
    }


    @Test
    void shouldThrowExceptionWhenStatusConversionIsNotAllowed(){
        shipment.setStatus(ShipmentStatus.CANCELED);

        when(shipmentRepository.findById(shipment.getId()))
                .thenReturn(Optional.of(shipment));

        UpdateStatusRequest request = new UpdateStatusRequest(shipment.getId(), ShipmentStatus.CREATED);

        InvalidStatusTransitionException exception = assertThrows(
                InvalidStatusTransitionException.class,
                () -> shipmentService.updateShipmentStatus(request)
        );
        assertEquals("Invalid status transition: CANCELED -> CREATED" , exception.getMessage());
    }

    @Test
    void shouldUpdateShipmentStatusWhenTransitionIsValid(){
        UpdateStatusRequest request = new UpdateStatusRequest(shipment.getId(), ShipmentStatus.PICKED_UP);

        when(shipmentRepository.findById(shipment.getId()))
                .thenReturn(Optional.of(shipment));
        int sizeBefore = shipment.getShipmentStatusHistories().size();
        shipmentService.updateShipmentStatus(request);
        int sizeAfter = shipment.getShipmentStatusHistories().size();
        assertEquals(request.status(), shipment.getStatus());
        assertEquals(sizeBefore + 1, sizeAfter);
    }

    @Test
    void shouldSaveShipmentWithRequestDataWhenAddShipment() {
        CreateShipmentRequest request = new CreateShipmentRequest(
                "Marrakech", "Casablanca", LocalDate.of(2026, 10, 25), 2.5
        );
        when(shipmentRepository.save(any(Shipment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShipmentResponse response = shipmentService.addShipment(request);

        ArgumentCaptor<Shipment> captor = ArgumentCaptor.forClass(Shipment.class);
        verify(shipmentRepository).save(captor.capture());
        Shipment saved = captor.getValue();

        assertEquals("Marrakech", saved.getOriginAddress());
        assertEquals("Casablanca", saved.getDestinationAddress());
        assertEquals(LocalDate.of(2026, 10, 25), saved.getPickupDate());
        assertEquals(BigDecimal.valueOf(2.5), saved.getWeightKg());
        assertNotNull(response);
        assertEquals("Marrakech", response.originAddress());
        assertEquals("Casablanca", response.destinationAddress());
    }

    @Test
    void shouldCreateInitialStatusHistoryWhenAddShipment() {
        CreateShipmentRequest request = new CreateShipmentRequest(
                "Marrakech", "Casablanca", LocalDate.of(2026, 10, 25), 2.5
        );
        when(shipmentRepository.save(any(Shipment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        shipmentService.addShipment(request);

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

    @Test
    void shouldReturnShipmentWhenTrackingNumberExists() {
        when(shipmentRepository.findByTrackingNumber(shipment.getTrackingNumber()))
                .thenReturn(Optional.of(shipment));

        ShipmentResponse response = shipmentService.getShipmentByTrackingNumber(shipment.getTrackingNumber());

        assertNotNull(response);
        assertEquals(shipment.getTrackingNumber(), response.trackingNumber());
        assertEquals("Marrakech", response.originAddress());
        assertEquals("Casablanca", response.destinationAddress());
        verify(shipmentRepository).findByTrackingNumber(shipment.getTrackingNumber());
    }

    @Test
    void shouldThrowExceptionWhenTrackingNumberDoesNotExist() {
        String trackingNumber = "TF-UNKNOWN";
        when(shipmentRepository.findByTrackingNumber(trackingNumber))
                .thenReturn(Optional.empty());

        ShipmentNotFoundException exception = assertThrows(
                ShipmentNotFoundException.class,
                () -> shipmentService.getShipmentByTrackingNumber(trackingNumber)
        );
        assertEquals("Shipment not found with tracking number: " + trackingNumber, exception.getMessage());
    }

    @Test
    void shouldRecordFromAndToStatusInHistoryWhenTransitionIsValid() {
        ShipmentStatus previous = shipment.getStatus();
        UpdateStatusRequest request = new UpdateStatusRequest(shipment.getId(), ShipmentStatus.PICKED_UP);
        when(shipmentRepository.findById(shipment.getId()))
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
        when(shipmentRepository.findById(shipment.getId()))
                .thenReturn(Optional.of(shipment));
        UpdateStatusRequest request = new UpdateStatusRequest(shipment.getId(), ShipmentStatus.CREATED);

        assertThrows(InvalidStatusTransitionException.class,
                () -> shipmentService.updateShipmentStatus(request));

        assertEquals(ShipmentStatus.CANCELED, shipment.getStatus());
        assertTrue(shipment.getShipmentStatusHistories().isEmpty());
    }

    @Test
    void shouldNotCallSaveWhenUpdateShipmentStatus() {
        UpdateStatusRequest request = new UpdateStatusRequest(shipment.getId(), ShipmentStatus.PICKED_UP);
        when(shipmentRepository.findById(shipment.getId()))
                .thenReturn(Optional.of(shipment));

        shipmentService.updateShipmentStatus(request);

        verify(shipmentRepository, never()).save(any());
    }
}
