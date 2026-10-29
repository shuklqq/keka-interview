package com.ranjeet.interview.repository;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Storage contract every entity repository extends. Services depend on the entity-specific
 * interface (e.g. TaskRepository) only, so swapping the in-memory implementation for a
 * database-backed one touches nothing above the repository.
 *
 * <p>Deliberately shaped like Spring Data's CrudRepository: when JPA arrives, the entity
 * interface extends JpaRepository instead and the service does not change.
 */
public interface BaseRepository<T, ID> {

    T save(T entity);

    Optional<T> findById(ID id);

    List<T> findAll();

    /**
     * Atomic read-modify-write of one entity: the check and the write happen with no window in
     * between. The change function must be pure (no I/O, no logging) and must not return null.
     * In memory this is ConcurrentHashMap.compute; on a database it is a transaction with a row
     * lock (or an optimistic @Version check).
     *
     * @throws com.ranjeet.interview.exception.NotFoundException if no entity has this id
     */
    T update(ID id, UnaryOperator<T> change);

    void deleteById(ID id);
}
