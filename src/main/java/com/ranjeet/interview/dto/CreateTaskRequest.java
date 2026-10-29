package com.ranjeet.interview.dto;

import com.ranjeet.interview.entity.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Inbound JSON shape. Validation rules are declared on the fields; the handler turns failures into 400. */
public record CreateTaskRequest(
        @NotBlank(message = "title is required") @Size(max = 200) String title,
        @Size(max = 2000) String description,
        TaskPriority priority) {
}
