package com.ranjeet.interview.entity;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class Alert {
    String id;
    String serviceId;
    Severity severity;
    String title;
    String description;
    String dedupKey;
    Instant timestamp;
    IncidentStatus status;
    Instant createdAt;
    Notification notification;

    public Alert withNotification(Notification notification) {
        IncidentStatus next = notification.getStatus() == NotificationStatus.SENT ? IncidentStatus.NOTIFIED : status;
        return toBuilder().notification(notification).status(next).build();
    }
}
