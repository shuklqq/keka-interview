package com.ranjeet.interview.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ranjeet.interview.dto.CreateTaskRequest;
import com.ranjeet.interview.entity.Task;
import com.ranjeet.interview.entity.TaskPriority;
import com.ranjeet.interview.entity.TaskStatus;
import com.ranjeet.interview.exception.ConflictException;
import com.ranjeet.interview.exception.ForbiddenException;
import com.ranjeet.interview.exception.NotFoundException;
import com.ranjeet.interview.service.TaskService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Web layer only: JSON contract, headers and status codes. The service is mocked. */
@WebMvcTest(TaskController.class)
class TaskControllerTest {

    private static final Instant NOW = Instant.parse("2026-10-27T10:00:00Z");
    private static final String VALID_JSON = """
            {"title": "Write template", "description": "desc", "priority": "HIGH"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    private static Task sampleTask(TaskStatus status) {
        return Task.builder()
                .id("t-1")
                .ownerId("u1")
                .title("Write template")
                .description("desc")
                .priority(TaskPriority.HIGH)
                .status(status)
                .createdAt(NOW)
                .build();
    }

    @Test
    void create_returns201WithLocationAndBody() throws Exception {
        when(taskService.create(eq("u1"), any(CreateTaskRequest.class))).thenReturn(sampleTask(TaskStatus.NEW));

        mockMvc.perform(post("/api/v1/tasks")
                        .header("X-User-Id", "u1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/tasks/t-1")))
                .andExpect(jsonPath("$.id").value("t-1"))
                .andExpect(jsonPath("$.ownerId").value("u1"))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.createdAt").value("2026-10-27T10:00:00Z"));
    }

    @Test
    void create_missingUserHeader_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("X-User-Id")));
        verifyNoInteractions(taskService);
    }

    @Test
    void create_blankTitle_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/v1/tasks")
                        .header("X-User-Id", "u1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.title").value("title is required"))
                .andExpect(jsonPath("$.path").value("/api/v1/tasks"));
        verifyNoInteractions(taskService);
    }

    @Test
    void create_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/tasks")
                        .header("X-User-Id", "u1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request"));
    }

    @Test
    void create_noBody_returns400SayingBodyRequired() throws Exception {
        mockMvc.perform(post("/api/v1/tasks")
                        .header("X-User-Id", "u1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is required"));
    }

    @Test
    void create_formBody_returns415() throws Exception {
        mockMvc.perform(post("/api/v1/tasks")
                        .header("X-User-Id", "u1")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .content("title=Write+template"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.message").value(containsString("application/json")));
    }

    @Test
    void get_existingId_returns200() throws Exception {
        when(taskService.get("t-1")).thenReturn(sampleTask(TaskStatus.NEW));

        mockMvc.perform(get("/api/v1/tasks/t-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("t-1"))
                .andExpect(jsonPath("$.title").value("Write template"));
    }

    @Test
    void get_unknownId_returns404() throws Exception {
        when(taskService.get("missing")).thenThrow(new NotFoundException("Task missing not found"));

        mockMvc.perform(get("/api/v1/tasks/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Task missing not found"))
                .andExpect(jsonPath("$.path").value("/api/v1/tasks/missing"));
    }

    @Test
    void complete_byOwner_returns200WithDoneStatus() throws Exception {
        when(taskService.complete("t-1", "u1")).thenReturn(sampleTask(TaskStatus.DONE));

        mockMvc.perform(post("/api/v1/tasks/t-1/complete").header("X-User-Id", "u1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    void complete_byOtherUser_returns403Json() throws Exception {
        when(taskService.complete("t-1", "u2")).thenThrow(new ForbiddenException("Only the owner can complete task t-1"));

        mockMvc.perform(post("/api/v1/tasks/t-1/complete").header("X-User-Id", "u2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Only the owner can complete task t-1"));
    }

    @Test
    void complete_alreadyDone_returns409Json() throws Exception {
        when(taskService.complete("t-1", "u1")).thenThrow(new ConflictException("Task t-1 is already DONE"));

        mockMvc.perform(post("/api/v1/tasks/t-1/complete").header("X-User-Id", "u1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Task t-1 is already DONE"));
    }

    @Test
    void unknownRoute_returns404Json() throws Exception {
        mockMvc.perform(get("/api/v1/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("No endpoint for GET /api/v1/nope"));
    }

    @Test
    void unsupportedMethod_returns405Json() throws Exception {
        mockMvc.perform(delete("/api/v1/tasks/t-1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }
}
