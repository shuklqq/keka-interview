# Pattern index: copy from the Task slice

The Task slice is the reference. For each pattern, open the file named below, copy it into a new file,
rename the type, and edit the fields. Say it aloud: "reusing the shape from my template's example".
Compile after every file. Order: plan comment, entity, repository, service, controller, Postman.

## 0. Plan comment (minute 0 to 5, top of the service file, before any code)

```java
// Entities : Task(id, ownerId, title, priority, status, createdAt)
// Endpoints:
//   POST /api/v1/tasks                X-User-Id   201 | 400 | 415
//   GET  /api/v1/tasks/{id}                       200 | 404
//   POST /api/v1/tasks/{id}/complete  X-User-Id   200 | 403 | 404 | 409   -> notifies downstream, best effort
// Transitions: NEW | IN_PROGRESS -> DONE ; DONE is terminal
// Assumptions: gateway sets X-User-Id; one notification attempt, failure logged; ids are UUIDs
```

## 1. The five patterns, all in working code

| Pattern | Copy from | What changes |
|---|---|---|
| Entity: `@Value @Builder(toBuilder = true)`, status enum, transition method that throws 409 | `entity/Task.java`, `entity/TaskStatus.java` | fields, enum values, the transition name and rule |
| Request record with validation; response record with `from(entity)` | `dto/CreateTaskRequest.java`, `dto/TaskResponse.java` | fields and constraints |
| Repository: interface extends `BaseRepository`, in-memory class with one constructor and a finder | `repository/TaskRepository.java`, `repository/InMemoryTaskRepository.java` | entity type, id getter, finder name |
| Service: defaults once in `create`; owner check + transition inside `repository.update`; best-effort outbound call | `service/TaskService.java` | the rule inside the `update` lambda |
| Controller: `@RequestHeader` actor, POST 201 + Location, GET by id, optional `@RequestParam`, transition POST | `controller/TaskController.java` | paths, names |

Also in code: outbound call `client/ExternalServiceClient.notifyTaskCompleted` (copy, rename, change path and body);
error mapping `exception/GlobalExceptionHandler` (nothing to change; throw the exceptions below);
tests `service/TaskServiceTest` (how to stub `update`) and `controller/TaskControllerTest` (status codes and headers).

## 2. Throwing (never catch in a controller)

```java
throw new NotFoundException("Order " + id + " not found");        // 404
throw new ForbiddenException("Only the owner can cancel");          // 403
throw new ConflictException("Cannot cancel from " + status);         // 409
throw new BadRequestException("from must not be after to");         // 400 for a rule annotations cannot express
throw new ApiException(HttpStatus.GONE, "request expired");          // any other status, no new class
```

## 3. Patterns the Task slice does not contain

**Seed data** (in the in-memory repository):

```java
@PostConstruct   // jakarta.annotation.PostConstruct
void seed() {
    save(Account.builder().id("a1").managerId("m1").build());
    save(Account.builder().id("m1").build());
}
```

**Balance-style rule** (wallet, inventory, leave balance): a mutable aggregate whose check and change are one method.
`@Getter @Builder`, no hand-written constructor, `@Builder.Default` for the initial value, mutated only inside `repository.update`.

```java
@Getter
@Builder
public class Account {
    String id;
    @Builder.Default
    Map<Category, Integer> balance = new EnumMap<>(Map.of(Category.A, 10, Category.B, 5));

    /** Check, then change, in one place. Throws instead of half-applying. */
    public void reserve(Category category, int units) {
        int left = balance.getOrDefault(category, 0);
        if (left < units) {
            throw new ConflictException(category + " balance " + left + " < " + units);
        }
        balance.put(category, left - units);
    }

    public void release(Category category, int units) {
        balance.merge(category, units, Integer::sum);
    }
}
```

Service side: `accounts.update(id, a -> { a.reserve(req.category(), units); return a; });` then save the request.

**Dates:**

```java
int days = (int) ChronoUnit.DAYS.between(from, to) + 1;          // inclusive calendar days
boolean overlap = !aFrom.isAfter(bTo) && !bFrom.isAfter(aTo);     // two closed ranges
if (from.isAfter(to)) throw new BadRequestException("from must not be after to");
```

## 4. Testing it by hand

Postman: import `postman/interview-template.postman_collection.json`, run "Create task" first, then the rest.
For a new endpoint, duplicate the closest request, change the URL, body and header. The header `X-User-Id`
is the acting user; change its value to test 403s.

```bash
curl -s -i -X POST localhost:8080/api/v1/tasks -H 'Content-Type: application/json' -H 'X-User-Id: u1' \
  -d '{"title":"Write template","priority":"HIGH"}'
curl -s -X POST localhost:8080/api/v1/tasks/<id>/complete -H 'X-User-Id: u1'
```
