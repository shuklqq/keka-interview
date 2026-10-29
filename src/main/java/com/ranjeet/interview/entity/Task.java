package com.ranjeet.interview.entity;

import com.ranjeet.interview.exception.ConflictException;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;

/**
 * Immutable domain object. Independent of JSON shape and of storage.
 * A state change returns a new instance, and the entity owns its own transition rule,
 * so no caller can move a task into an illegal state.
 */
@Value
@Builder(toBuilder = true)
public class Task {
    String id;
    String ownerId;
    String title;
    String description;
    TaskPriority priority;
    TaskStatus status;
    Instant createdAt;

    /** NEW or IN_PROGRESS -> DONE. Already DONE is a 409: the request is valid, the current state forbids it. */
    public Task complete() {
        if (status == TaskStatus.DONE) {
            throw new ConflictException("Task " + id + " is already DONE");
        }
        return toBuilder().status(TaskStatus.DONE).build();
    }
}
