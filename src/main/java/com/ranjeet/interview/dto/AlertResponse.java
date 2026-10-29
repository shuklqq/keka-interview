package com.ranjeet.interview.dto;

import com.ranjeet.interview.entity.Alert;
import com.ranjeet.interview.entity.IncidentStatus;
import com.ranjeet.interview.entity.Notification;
import com.ranjeet.interview.entity.Severity;
import java.time.Instant;

public record AlertResponse(
        String incidentId,
        String serviceId,
        Severity severity,
        String title,
        String description,
        String dedupKey,
        Instant timestamp,
        IncidentStatus status,
        Instant createdAt,
        Notification notification) {

    public static AlertResponse from(Alert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getServiceId(),
                alert.getSeverity(),
                alert.getTitle(),
                alert.getDescription(),
                alert.getDedupKey(),
                alert.getTimestamp(),
                alert.getStatus(),
                alert.getCreatedAt(),
                alert.getNotification());
    }
}
