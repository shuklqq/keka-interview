package com.ranjeet.interview.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ranjeet.interview.entity.Task;
import com.ranjeet.interview.entity.TaskPriority;
import com.ranjeet.interview.entity.TaskStatus;
import com.ranjeet.interview.exception.ConflictException;
import com.ranjeet.interview.exception.NotFoundException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class InMemoryTaskRepositoryTest {

    private final InMemoryTaskRepository repository = new InMemoryTaskRepository();

    private static Task task(String id) {
        return task(id, "u1");
    }

    private static Task task(String id, String ownerId) {
        return Task.builder().id(id).ownerId(ownerId).title("t").priority(TaskPriority.LOW)
                .status(TaskStatus.NEW).createdAt(Instant.EPOCH).build();
    }

    @Test
    void save_thenFindById_returnsSameTask() {
        Task saved = repository.save(task("a"));

        assertThat(repository.findById("a")).contains(saved);
        assertThat(repository.findById("zzz")).isEmpty();
    }

    @Test
    void findAll_returnsEverySavedTask() {
        repository.save(task("a"));
        repository.save(task("b"));

        assertThat(repository.findAll()).extracting(Task::getId).containsExactlyInAnyOrder("a", "b");
    }

    @Test
    void update_appliesChangeAndStoresResult() {
        repository.save(task("a"));

        Task updated = repository.update("a", t -> t.toBuilder().status(TaskStatus.DONE).build());

        assertThat(updated.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(repository.findById("a")).contains(updated);
    }

    @Test
    void update_unknownId_throwsNotFoundAndStoresNothing() {
        assertThatThrownBy(() -> repository.update("zzz", t -> t))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Task zzz not found");
        assertThat(repository.findById("zzz")).isEmpty();
    }

    @Test
    void update_whenChangeThrows_leavesEntityUnchanged() {
        Task saved = repository.save(task("a"));

        assertThatThrownBy(() -> repository.update("a", t -> {
            throw new ConflictException("rule violated");
        })).isInstanceOf(ConflictException.class);

        assertThat(repository.findById("a")).contains(saved);
    }

    @Test
    void update_returningNull_isRejectedAndEntityKept() {
        Task saved = repository.save(task("a"));

        assertThatThrownBy(() -> repository.update("a", t -> null)).isInstanceOf(NullPointerException.class);

        assertThat(repository.findById("a")).contains(saved);
    }

    @Test
    void findByOwnerId_returnsOnlyThatOwnersTasks() {
        repository.save(task("a", "u1"));
        repository.save(task("b", "u2"));
        repository.save(task("c", "u1"));

        assertThat(repository.findByOwnerId("u1")).extracting(Task::getId).containsExactlyInAnyOrder("a", "c");
        assertThat(repository.findByOwnerId("nobody")).isEmpty();
    }

    @Test
    void deleteById_removesTask() {
        repository.save(task("a"));

        repository.deleteById("a");

        assertThat(repository.findById("a")).isEmpty();
    }
}
