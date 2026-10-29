package com.ranjeet.interview.repository;

import com.ranjeet.interview.exception.NotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Thread-safe in-memory base for every repository. A concrete repository is a constructor:
 * extend this, pass the entity name and the id getter, implement the entity's interface.
 *
 * <p>save/findById/update/deleteById are O(1) average; findAll and stream() are O(n).
 */
public abstract class InMemoryRepository<T, ID> implements BaseRepository<T, ID> {

    private final String entityName;
    private final Function<T, ID> idOf;
    private final Map<ID, T> store = new ConcurrentHashMap<>();

    protected InMemoryRepository(String entityName, Function<T, ID> idOf) {
        this.entityName = entityName;
        this.idOf = idOf;
    }

    @Override
    public T save(T entity) {
        store.put(idOf.apply(entity), entity);
        return entity;
    }

    @Override
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<T> findAll() {
        return List.copyOf(store.values());
    }

    /**
     * ConcurrentHashMap.compute invokes the function exactly once, under the lock for this key, so
     * the check and the write cannot interleave with another thread's update of the same entity.
     * Throwing inside the function leaves the map unchanged. Returning null would remove the
     * entry, so it is rejected.
     */
    @Override
    public T update(ID id, UnaryOperator<T> change) {
        return store.compute(id, (key, current) -> {
            if (current == null) {
                throw new NotFoundException(entityName + " " + id + " not found");
            }
            return Objects.requireNonNull(change.apply(current), "update must not return null");
        });
    }

    @Override
    public void deleteById(ID id) {
        store.remove(id);
    }

    /** For custom finders in subclasses: {@code stream().filter(t -> ...).toList()}. Weakly consistent, O(n). */
    protected Stream<T> stream() {
        return store.values().stream();
    }
}
