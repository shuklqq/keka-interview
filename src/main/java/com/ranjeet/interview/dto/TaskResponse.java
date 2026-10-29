package com.ranjeet.interview.dto;

import com.ranjeet.interview.entity.Task;
import com.ranjeet.interview.entity.TaskPriority;
import com.ranjeet.interview.entity.TaskStatus;
import java.time.Instant;

/** Outbound JSON shape. Kept separate from the domain so either can change independently. */
public record TaskResponse(
        String id,
        String ownerId,
        String title,
        String description,
        TaskPriority priority,
        TaskStatus status,
        Instant createdAt) {

    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getOwnerId(),
                task.getTitle(),
                task.getDescription(),
                task.getPriority(),
                task.getStatus(),
                task.getCreatedAt());
    }
}
