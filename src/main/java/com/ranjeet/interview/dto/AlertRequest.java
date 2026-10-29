package com.ranjeet.interview.dto;

import com.ranjeet.interview.entity.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record AlertRequest(
        @NotBlank String serviceId,
        @NotNull Severity severity,
        @NotBlank String title,
        @Size(max = 2000) String description,
        @NotBlank String dedupKey,
        @NotNull Instant timestamp) {
}
