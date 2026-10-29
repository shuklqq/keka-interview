package com.ranjeet.interview.client;

import java.time.Instant;
import java.util.Map;

/**
 * What the downstream system returns when we notify it.
 * We own this type: it mirrors only the fields we need, so their changes do not ripple into us.
 */
public record Acknowledgement(Instant receivedAt, Map<String, Object> received) {
}
