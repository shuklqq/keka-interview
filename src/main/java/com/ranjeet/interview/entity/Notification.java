package com.ranjeet.interview.entity;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Notification {
    String channel;
    NotificationStatus status;
    String providerCallId;
    String lastError;
}
