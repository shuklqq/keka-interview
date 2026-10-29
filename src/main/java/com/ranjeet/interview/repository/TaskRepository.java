package com.ranjeet.interview.repository;

import com.ranjeet.interview.entity.Task;
import java.util.List;

/**
 * Storage contract for tasks. save/findById/findAll/update/deleteById come from BaseRepository.
 * Entity-specific finders are declared here; Spring Data would derive them from the name.
 */
public interface TaskRepository extends BaseRepository<Task, String> {

    List<Task> findByOwnerId(String ownerId);
}
