package com.TrackFlow.shipment;

import com.TrackFlow.shipment.dtos.entitiesDTO.CustomerDTO;
import com.TrackFlow.shipment.dtos.requests.CreateShipmentRequest;
import com.TrackFlow.shipment.dtos.response.ShipmentResponse;
import com.TrackFlow.shipment.entities.enums.IdType;
import com.TrackFlow.shipment.repositories.ShipmentRepository;
import com.TrackFlow.shipment.services.interfaces.ShipmentService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
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

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class ShipmentEventIT {

    @Autowired
    private ShipmentService shipmentService;

    @Autowired
    private ShipmentRepository shipmentRepository;

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
    void shouldPublishShipmentCreatedEventToKafka() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of("shipment.created"));

            ShipmentResponse created = shipmentService.addShipment(new CreateShipmentRequest(
                    "Marrakech", "Casablanca", LocalDate.of(2026, 10, 25), 2.5,
                    new CustomerDTO(IdType.CIN, "EE989898", "test_name", "test_last_name",
                            "0607080900", "test@gmail.com")
            ));

            ConsumerRecord<String, String> found = null;
            long deadline = System.currentTimeMillis() + 15_000;
            while (found == null && System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> record : records) {
                    if (created.trackingNumber().equals(record.key())) {
                        found = record;
                    }
                }
            }

            assertNotNull(found, "No event received for " + created.trackingNumber());
            assertTrue(found.value().contains(created.trackingNumber()));
            assertTrue(found.value().contains("Marrakech"));
        }
    }
}