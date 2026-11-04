package com.TrackFlow.shipment.events.publishers;

import com.TrackFlow.shipment.events.ShipmentCreatedEvent;
import com.TrackFlow.shipment.events.ShipmentStatusChangedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ShipmentEventPublisher {
    private static final String SHIPMENT_CREATED_TOPIC = "shipment.created";
    private static final String SHIPMENT_STATUS_CHANGED_TOPIC = "shipment.status-changed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ShipmentEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onShipmentCreated(ShipmentCreatedEvent event) {
        kafkaTemplate.send(SHIPMENT_CREATED_TOPIC, event.trackingNumber(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onShipmentStatusChanged(ShipmentStatusChangedEvent event){
        kafkaTemplate.send(SHIPMENT_STATUS_CHANGED_TOPIC, event.trackingNumber(), event);
    }
}
