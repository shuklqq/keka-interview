package com.ranjeet.interview.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ranjeet.interview.client.ExternalServiceClient;
import com.ranjeet.interview.dto.CreateTaskRequest;
import com.ranjeet.interview.entity.Task;
import com.ranjeet.interview.entity.TaskPriority;
import com.ranjeet.interview.entity.TaskStatus;
import com.ranjeet.interview.exception.ConflictException;
import com.ranjeet.interview.exception.ExternalCallException;
import com.ranjeet.interview.exception.ForbiddenException;
import com.ranjeet.interview.exception.NotFoundException;
import com.ranjeet.interview.repository.TaskRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Business rules only: no Spring context, no HTTP. */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-27T10:00:00Z");

    @Mock
    private TaskRepository repository;

    @Mock
    private ExternalServiceClient externalServiceClient;

    private TaskService service;

    @BeforeEach
    void setUp() {
        service = new TaskService(repository, externalServiceClient, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static Task task(TaskStatus status) {
        return Task.builder().id("t-1").ownerId("u1").title("x").priority(TaskPriority.LOW)
                .status(status).createdAt(NOW).build();
    }

    /** Makes the mocked repository.update behave like the real one: apply the function to the stored task. */
    private void storedTaskIs(Task current) {
        when(repository.update(eq("t-1"), any()))
                .thenAnswer(inv -> inv.<UnaryOperator<Task>>getArgument(1).apply(current));
    }

    @Test
    void create_setsOwnerAndDefaultsThenSaves() {
        when(repository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task created = service.create("u1", new CreateTaskRequest("Write template", null, null));

        assertThat(created.getId()).isNotBlank();
        assertThat(created.getOwnerId()).isEqualTo("u1");
        assertThat(created.getTitle()).isEqualTo("Write template");
        assertThat(created.getPriority()).isEqualTo(TaskService.DEFAULT_PRIORITY);
        assertThat(created.getStatus()).isEqualTo(TaskStatus.NEW);
        assertThat(created.getCreatedAt()).isEqualTo(NOW);
        verify(repository).save(created);
    }

    @Test
    void create_keepsExplicitPriority() {
        when(repository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task created = service.create("u1", new CreateTaskRequest("Urgent", "now", TaskPriority.HIGH));

        assertThat(created.getPriority()).isEqualTo(TaskPriority.HIGH);
    }

    @Test
    void get_unknownId_throwsNotFound() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get("missing"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void complete_byOwner_marksDoneAndNotifies() {
        storedTaskIs(task(TaskStatus.NEW));

        Task done = service.complete("t-1", "u1");

        assertThat(done.getStatus()).isEqualTo(TaskStatus.DONE);
        verify(externalServiceClient).notifyTaskCompleted(argThat(t -> t.getStatus() == TaskStatus.DONE));
    }

    @Test
    void complete_byOtherUser_throwsForbiddenAndDoesNotNotify() {
        storedTaskIs(task(TaskStatus.NEW));

        assertThatThrownBy(() -> service.complete("t-1", "u2"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("owner");
        verifyNoInteractions(externalServiceClient);
    }

    @Test
    void complete_alreadyDone_throwsConflict() {
        storedTaskIs(task(TaskStatus.DONE));

        assertThatThrownBy(() -> service.complete("t-1", "u1"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already DONE");
        verifyNoInteractions(externalServiceClient);
    }

    @Test
    void complete_whenNotificationFails_stillReturnsDone() {
        storedTaskIs(task(TaskStatus.NEW));
        when(externalServiceClient.notifyTaskCompleted(any())).thenThrow(new ExternalCallException("downstream down", null));

        Task done = service.complete("t-1", "u1");

        assertThat(done.getStatus()).isEqualTo(TaskStatus.DONE);
    }
}
