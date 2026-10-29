package com.ranjeet.interview.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Bound from the {@code external.*} keys in application.yml.
 * Defaults are declared here, once, and only apply when the key is absent.
 */
@ConfigurationProperties(prefix = "external")
public record ExternalServiceProperties(
        String baseUrl,
        @DefaultValue("3s") Duration connectTimeout,
        @DefaultValue("5s") Duration readTimeout) {
}
