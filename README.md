# interview-template

Runnable Spring Boot service to start a hands-on coding round from minute one.
Copy it, run it, then build the interview problem by copying the Task slice, one file at a time.
`SNIPPETS.md` is the pattern index: which file to copy for which pattern.

| Piece | Choice |
|---|---|
| Framework | Spring Boot 3.5.16 (Spring MVC, embedded Tomcat) |
| Language level | Java 21, runs on any JDK 17+ on the PATH |
| Build | Maven via `./mvnw` (no global Maven needed) |
| Dev loop | Spring Boot DevTools: save in the editor, the app restarts itself in about two seconds |
| Editor | VS Code with the Java extension pack; settings committed in `.vscode/settings.json` |
| Outbound HTTP | `RestClient` with connect/read timeouts (`config/HttpClientConfig`) |
| Logging | SLF4J + Logback, `@Slf4j`, pattern and levels in `application.yml` |
| Boilerplate | Lombok (`@Slf4j`, `@RequiredArgsConstructor`, `@Value`, `@Builder`) + Java records |
| Validation | Jakarta Bean Validation, errors mapped to JSON 400 in `GlobalExceptionHandler` |
| Errors | `ApiException(status, message)` family, one handler for all of them |
| Storage | `InMemoryRepository<T, ID>` base (ConcurrentHashMap, atomic `update`) behind per-entity interfaces |
| Tests | JUnit 5, Mockito, AssertJ, MockMvc |

## What these rounds judge

From candidate guides and hiring write-ups on machine-coding rounds, in the order they weigh:

1. **It runs.** Working functionality first; a non-working solution scores low regardless of code quality.
2. **Structure and readability.** Small methods, one concern each, names that explain themselves, no duplication, layers kept apart.
3. **Extensibility.** Interfaces where a second implementation is plausible (storage, downstream client), rules in one place.
4. **Edge cases and errors.** Validation, not-found, forbidden, conflicts, failed downstream calls, each with the right status.
5. **Communication.** A plan before code, trade-offs said aloud, assumptions stated.
6. **Tests**, if time remains, on the rule that matters most.

Red flags: one giant method, no edge cases, silence while typing.

## Run

```bash
./mvnw spring-boot:run            # start on http://localhost:8080
./mvnw test                       # unit + web-slice + context tests
./mvnw -q package && java -jar target/*.jar
```

Start it once and leave it running. DevTools watches `target/classes`, so every editor save restarts the app
in place (about two seconds) and in-memory data is re-seeded. Do not run `./mvnw clean` or `./mvnw test`
while it is up: clean deletes the classes the running app is using.

Test with Postman (import `postman/interview-template.postman_collection.json`, run "Create task" first),
with `requests.http` (VS Code REST Client extension), or with curl:

```bash
curl -s localhost:8080/actuator/health
curl -s -i -X POST localhost:8080/api/v1/tasks -H 'Content-Type: application/json' -H 'X-User-Id: u1' \
  -d '{"title":"Write template","description":"first task","priority":"HIGH"}'                        # 201
curl -s localhost:8080/api/v1/tasks/<id>                                                               # 200 / 404
curl -s 'localhost:8080/api/v1/tasks?ownerId=u1'
curl -s -X POST localhost:8080/api/v1/tasks/<id>/complete -H 'X-User-Id: u1'                           # 200, notifies downstream
curl -s -X POST localhost:8080/api/v1/tasks/<id>/complete -H 'X-User-Id: u2'                           # 403
curl -s -X POST localhost:8080/api/v1/tasks/<id>/complete -H 'X-User-Id: u1'                           # 409, already DONE
```

## Endpoints

| Method | Path | Purpose | Status |
|---|---|---|---|
| POST | `/api/v1/tasks` | Create a task for the `X-User-Id` caller | 201 + `Location`; 400 validation, missing header or body; 415 wrong content type |
| GET | `/api/v1/tasks/{id}` | Fetch one | 200, 404 |
| GET | `/api/v1/tasks?ownerId=` | List all, or one owner's | 200 |
| POST | `/api/v1/tasks/{id}/complete` | Owner marks it DONE; downstream notified once, best effort | 200; 403 not owner; 404; 409 already done |
| POST | `/internal/echo` | Stand-in for the downstream system | 200 |
| GET | `/actuator/health` | Liveness | 200 |

Error shape, always the same:

```json
{ "timestamp": "...", "status": 400, "error": "Bad Request", "message": "Validation failed",
  "path": "/api/v1/tasks", "fieldErrors": { "title": "title is required" } }
```

## Layout: one job per class

```
src/main/java/com/ranjeet/interview/
├── InterviewApplication.java          starts Spring Boot
├── controller/
│   ├── TaskController.java            HTTP in/out only: header, @Valid, delegate, map, status
│   └── EchoController.java            fake downstream system for the outbound call
├── dto/                               CreateTaskRequest, TaskResponse, ErrorResponse, EchoResponse
├── service/TaskService.java           rules: defaults once; owner check + transition inside update; best-effort notify
├── entity/                            Task (immutable, owns its transition), TaskStatus, TaskPriority
├── repository/
│   ├── BaseRepository.java            save / findById / findAll / update(id, fn) / deleteById
│   ├── InMemoryRepository.java        ConcurrentHashMap implementation of the above, generic
│   ├── TaskRepository.java            Task contract: extends BaseRepository, adds findByOwnerId
│   └── InMemoryTaskRepository.java    one constructor plus the finder
├── exception/
│   ├── GlobalExceptionHandler.java    every exception -> ErrorResponse JSON, in one place
│   ├── ApiException.java              (status, message) base; handled once
│   ├── NotFoundException.java         -> 404
│   ├── BadRequestException.java       -> 400 (semantic input errors)
│   ├── ForbiddenException.java        -> 403 (known caller, not allowed)
│   ├── ConflictException.java         -> 409 (state rule violated)
│   └── ExternalCallException.java     -> 502
├── client/
│   ├── ExternalServiceClient.java     outbound calls; HTTP errors -> ExternalCallException
│   └── Acknowledgement.java           what the downstream returns (we own the type)
└── config/
    ├── AppConfig.java                 Clock bean (injectable "now")
    ├── ExternalServiceProperties.java external.* keys, defaults declared once
    └── HttpClientConfig.java          the one RestClient bean: base URL + timeouts
postman/interview-template.postman_collection.json   every request above, including the error cases
```

Request flow: JSON + header -> `TaskController` -> validation -> `TaskService` -> `TaskRepository` -> back out as `TaskResponse`.

## Building the interview problem

Work in one vertical slice: the highest-priority use case end to end, tested, before anything else.
Each step is "copy the Task file, rename, edit"; `SNIPPETS.md` maps patterns to files.

1. Plan comment: entities, endpoints with status codes, transitions, assumptions. Five minutes, no code.
2. Entity: copy `Task`. Enum for every closed set, `@Value @Builder(toBuilder = true)`, the transition method with its 409 rule.
3. Repository: copy `TaskRepository` and `InMemoryTaskRepository`. Seed data in `@PostConstruct` if the problem needs fixtures.
4. Service: copy `TaskService`. Defaults in `create`; every check-and-change inside `repository.update`.
5. Request record and controller: copy `CreateTaskRequest` and `TaskController`. Header for the acting user.
6. Run it in Postman: the happy path, then each error status. Only now the next use case.
7. Outbound call: copy `notifyTaskCompleted` in the client; call it best effort after the state change.
8. Tests last: copy `TaskServiceTest` for the rule, `TaskControllerTest` for the contract.

### Moving to a real database

The seam is the per-entity repository interface. The migration checklist (rehearse saying it aloud):

- Add `spring-boot-starter-data-jpa` and a driver (H2 for local, PostgreSQL for real).
- Simplest: annotate `Task` with `@Entity` / `@Id` (Lombok `@Value` must become `@Getter @Setter @NoArgsConstructor` since JPA needs a no-args constructor and mutability). Purer: keep `Task` as is and add a separate `TaskEntity` with a mapper.
- `TaskRepository extends JpaRepository<Task, String>` replaces `extends BaseRepository`; Spring Data generates save/findById/findAll and `findByOwnerId` from the name.
- `update(id, fn)` becomes a `@Transactional` service method: `findById` under `@Lock(PESSIMISTIC_WRITE)` (or a `@Version` column for optimistic locking), apply the change, save. Same shape, database-held lock instead of the map's bin lock.
- `InMemoryTaskRepository` goes away or stays behind `@Profile("local")`.
- Nothing in `TaskService`, `TaskController` or the DTOs changes. That is the point of the interface.

## 60-minute playbook

| Minutes | Do |
|---|---|
| 0-5 | Restate the problem. Ask: inputs, outputs, edge cases, what "done" means. Write the plan comment. No code. |
| 5-20 | Slice 1: the highest-priority use case, entity to Postman. **Hard checkpoint: a 201 by minute 20.** |
| 20-35 | The second use case (usually a state transition with a 403 and a 409), then the outbound call. |
| 35-45 | Remaining rules and error cases. Every status code in the plan comment exercised once. |
| 45-52 | Cleanup, one test if everything runs. |
| 52-60 | Walk through: structure, concurrency, errors, what changes for a DB. `git add -A && git commit && git push`. Share the link. |

Say what you are doing while you do it. A clean partial beats a messy complete.

### Speed rules (decided now, never debated live)

- String UUID ids. Records for request DTOs. `@Value @Builder(toBuilder = true)` entities. No hand-written constructors.
- An enum for every closed set; Jackson rejects bad values with a 400 for free.
- The entity owns its state rule (transition method); the service owns authorization and orchestration.
- State transitions via `toBuilder()`. A mutable aggregate only for a balance-style rule, mutated only inside `repository.update`.
- Return a response record from the controller; copy `TaskResponse`, it is eight lines.
- One exception class per status, mapped once. Controllers never try/catch.
- No tests before the happy path runs end to end.
- Copy, rename, edit. Type the domain, never the plumbing. Decide alone; there is nobody to ask.

## Pre-interview checklist (VS Code)

- [ ] AI off: disable the **GitHub Copilot** and **GitHub Copilot Chat** extensions (and any other AI completion extension). Do not use Cursor. Close Claude.
- [ ] Fresh copy from `../new-interview.sh <name>`, then **open that folder as the window root** (File, Open Folder). The pom must be at the root or the Java language server will not import the project.
- [ ] Java Projects view shows the project with a "Maven Dependencies" node; Problems panel is empty. If not: Command Palette, "Java: Import Java Projects into Workspace".
- [ ] Type a class name, press Enter on the suggestion, and confirm the import appears. Cmd+. on a red name offers "Import". Shift+Alt+O organizes imports.
- [ ] New files via "Java: New Java File" or right-click a package in Java Projects, so the package line is generated.
- [ ] `./mvnw test` passes (warms the dependency cache so the live build is fast).
- [ ] `./mvnw spring-boot:run` is up and stays up (DevTools restarts it on save).
- [ ] Postman open with the collection imported; `SNIPPETS.md` open in a split.
- [ ] Browser tabs: Spring Boot reference, Spring Framework `RestClient` docs, Jakarta Validation constraints, Baeldung.
- [ ] `git status` clean, GitHub remote ready to push after.

## Cheat sheet

```java
// RestClient GET / POST
Foo foo = restClient.get().uri("/foo/{id}", id).retrieve().body(Foo.class);
Bar bar = restClient.post().uri("/bar").contentType(APPLICATION_JSON).body(req).retrieve().body(Bar.class);
List<Foo> foos = restClient.get().uri("/foo").retrieve().body(new ParameterizedTypeReference<>() {});

// Validation on records
record Req(@NotBlank String name, @Min(1) int qty, @Email String email, @NotNull @Valid Address address) {}

// Query params, headers, status
@GetMapping public List<X> list(@RequestParam(defaultValue = "0") int page, @RequestHeader("X-Tenant") String t)
@ResponseStatus(HttpStatus.NO_CONTENT) @DeleteMapping("/{id}") public void delete(@PathVariable String id)

// Atomic check-and-change on one entity (throw inside to reject; map stays unchanged)
repo.update(id, e -> { if (!e.getOwnerId().equals(actor)) throw new ForbiddenException("..."); return e.complete(); });

// Other concurrency-safe in-memory patterns
map.computeIfAbsent(key, k -> new ArrayList<>()).add(v);      // group by key
map.compute(key, (k, v) -> v == null ? 1 : v + 1);             // counter
AtomicLong seq = new AtomicLong(); String id = "T-" + seq.incrementAndGet();

// Dates and time
int days = (int) ChronoUnit.DAYS.between(from, to) + 1;        // inclusive calendar days
boolean overlap = !aFrom.isAfter(bTo) && !bFrom.isAfter(aTo);  // closed ranges
Instant now = Instant.now(clock);  LocalDate d = LocalDate.now(clock);  Duration.between(a, b)
```
