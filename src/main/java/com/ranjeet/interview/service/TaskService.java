package com.ranjeet.interview.service;

import com.ranjeet.interview.client.ExternalServiceClient;
import com.ranjeet.interview.dto.CreateTaskRequest;
import com.ranjeet.interview.entity.Task;
import com.ranjeet.interview.entity.TaskPriority;
import com.ranjeet.interview.entity.TaskStatus;
import com.ranjeet.interview.exception.ExternalCallException;
import com.ranjeet.interview.exception.ForbiddenException;
import com.ranjeet.interview.exception.NotFoundException;
import com.ranjeet.interview.repository.TaskRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Business rules live here. Controllers and repositories stay thin.
 * Defaults are set once, in create. Authorization (who may act) is checked here;
 * state rules (what the entity may become) are checked by the entity itself.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    static final TaskPriority DEFAULT_PRIORITY = TaskPriority.MEDIUM;

    private final TaskRepository repository;
    private final ExternalServiceClient externalServiceClient;
    private final Clock clock;

    public Task create(String ownerId, CreateTaskRequest request) {
        Task task = Task.builder()
                .id(UUID.randomUUID().toString())
                .ownerId(ownerId)
                .title(request.title())
                .description(request.description())
                .priority(Objects.requireNonNullElse(request.priority(), DEFAULT_PRIORITY))
                .status(TaskStatus.NEW)
                .createdAt(Instant.now(clock))
                .build();
        Task saved = repository.save(task);
        log.info("Task created id={} owner={} priority={}", saved.getId(), ownerId, saved.getPriority());
        return saved;
    }

    public Task get(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Task " + id + " not found"));
    }

    /** All tasks, or one owner's tasks when a filter is given. */
    public List<Task> list(String ownerId) {
        return ownerId == null ? repository.findAll() : repository.findByOwnerId(ownerId);
    }

    /**
     * Check-and-change in one atomic step: the owner check and the transition run inside
     * repository.update, so two concurrent calls cannot both succeed. A throw inside leaves
     * the task untouched. 404 (unknown id), 403 (not the owner) and 409 (already done) all come
     * out of the function; the controller never sees them because the handler maps them.
     */
    public Task complete(String id, String actorId) {
        Task done = repository.update(id, task -> {
            if (!task.getOwnerId().equals(actorId)) {
                throw new ForbiddenException("Only the owner can complete task " + id);
            }
            return task.complete();
        });
        notifyBestEffort(done);
        return done;
    }

    /** One attempt after the state change. Failure is logged, not propagated: the task is already done. */
    private void notifyBestEffort(Task task) {
        try {
            externalServiceClient.notifyTaskCompleted(task);
        } catch (ExternalCallException e) {
            log.warn("Notification for task {} failed: {}", task.getId(), e.getMessage());
        }
    }
}
