package com.ranjeet.interview.controller;

import com.ranjeet.interview.dto.CreateTaskRequest;
import com.ranjeet.interview.dto.TaskResponse;
import com.ranjeet.interview.entity.Task;
import com.ranjeet.interview.service.TaskService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * HTTP only: bind, validate, delegate, map, status code. No business rules here.
 * The acting user arrives in X-User-Id (a gateway would set it from the session); a missing header is a 400.
 */
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Slf4j
public class TaskController {

    static final String USER_HEADER = "X-User-Id";

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<TaskResponse> create(@RequestHeader(USER_HEADER) String userId,
                                               @Valid @RequestBody CreateTaskRequest request) {
        log.info("POST /api/v1/tasks user={} title='{}'", userId, request.title());
        Task created = taskService.create(userId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(TaskResponse.from(created));
    }

    @GetMapping("/{id}")
    public TaskResponse get(@PathVariable String id) {
        return TaskResponse.from(taskService.get(id));
    }

    /** GET /api/v1/tasks or GET /api/v1/tasks?ownerId=u1 */
    @GetMapping
    public List<TaskResponse> list(@RequestParam(required = false) String ownerId) {
        return taskService.list(ownerId).stream().map(TaskResponse::from).toList();
    }

    /** State transition as a POST on a sub-resource: 200 with the new state, 403 / 404 / 409 from the service. */
    @PostMapping("/{id}/complete")
    public TaskResponse complete(@PathVariable String id, @RequestHeader(USER_HEADER) String userId) {
        log.info("POST /api/v1/tasks/{}/complete user={}", id, userId);
        return TaskResponse.from(taskService.complete(id, userId));
    }
}
