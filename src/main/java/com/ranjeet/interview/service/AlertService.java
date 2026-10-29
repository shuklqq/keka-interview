package com.ranjeet.interview.service;

import com.ranjeet.interview.client.ExternalServiceClient;
import com.ranjeet.interview.dto.AlertRequest;
import com.ranjeet.interview.entity.Alert;
import com.ranjeet.interview.entity.IncidentStatus;
import com.ranjeet.interview.entity.Notification;
import com.ranjeet.interview.entity.NotificationStatus;
import com.ranjeet.interview.repository.AlertRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    static final String PHONE_CHANNEL = "PHONE";
    static final String SKIPPED_REASON = "severity below threshold";

    private final AlertRepository repository;
    private final ExternalServiceClient externalServiceClient;
    private final Clock clock;

    public Alert create(AlertRequest request) {
        Alert triggered = repository.save(newIncident(request));
        log.info("Incident created id={} service={} severity={}",
                triggered.getId(), triggered.getServiceId(), triggered.getSeverity());
        Notification outcome = notify(triggered);
        return outcome == null
                ? triggered
                : repository.update(triggered.getId(), alert -> alert.withNotification(outcome));
    }

    public List<Alert> list(IncidentStatus status) {
        return status == null ? repository.findAll() : repository.findByStatus(status);
    }

    private Alert newIncident(AlertRequest request) {
        return Alert.builder()
                .id(UUID.randomUUID().toString())
                .serviceId(request.serviceId())
                .severity(request.severity())
                .title(request.title())
                .description(request.description())
                .dedupKey(request.dedupKey())
                .timestamp(request.timestamp())
                .status(IncidentStatus.TRIGGERED)
                .createdAt(Instant.now(clock))
                .build();
    }

    private Notification notify(Alert alert) {
        return switch (alert.getSeverity()) {
            case INFO -> null;
            case WARNING -> skipped();
            case CRITICAL -> placeCall(alert);
        };
    }

    private Notification skipped() {
        return Notification.builder()
                .channel(PHONE_CHANNEL)
                .status(NotificationStatus.SKIPPED)
                .lastError(SKIPPED_REASON)
                .build();
    }

    private Notification placeCall(Alert alert) {
        log.warn("Exotel call for incident {} not placed: client not wired", alert.getId());
        return Notification.builder()
                .channel(PHONE_CHANNEL)
                .status(NotificationStatus.FAILED)
                .lastError("exotel client not wired")
                .build();
    }
}
