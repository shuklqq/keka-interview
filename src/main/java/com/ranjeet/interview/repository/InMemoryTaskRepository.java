package com.ranjeet.interview.repository;

import com.ranjeet.interview.entity.Task;
import java.util.List;
import org.springframework.stereotype.Repository;

/** In-memory tasks: the base class does the storage, this class adds the finders. */
@Repository
public class InMemoryTaskRepository extends InMemoryRepository<Task, String> implements TaskRepository {

    public InMemoryTaskRepository() {
        super("Task", Task::getId);
    }

    @Override
    public List<Task> findByOwnerId(String ownerId) {
        return stream().filter(task -> task.getOwnerId().equals(ownerId)).toList();
    }
}
