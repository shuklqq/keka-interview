package com.ranjeet.interview.dto;

import java.time.Instant;
import java.util.Map;

/** Returned by POST /internal/echo: the body we received and when. */
public record EchoResponse(Instant receivedAt, Map<String, Object> received) {
}
