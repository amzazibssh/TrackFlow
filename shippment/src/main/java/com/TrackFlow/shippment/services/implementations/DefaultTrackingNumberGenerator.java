package com.TrackFlow.shippment.services.implementations;

import com.TrackFlow.shippment.services.interfaces.TrackingNumberGenerator;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class DefaultTrackingNumberGenerator implements TrackingNumberGenerator {
    @Override
    public String generate() {
        String trackingNumber;
        // ta logique actuelle (TF-yyyyMMdd-XXXXXXXX), déplacée depuis @PrePersist

        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        trackingNumber = "TF-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + suffix;
        return trackingNumber;
    }
}