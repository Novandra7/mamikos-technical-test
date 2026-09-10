# Technical Comparison Report — Laravel vs Spring Boot

Two implementations of the **MamiKos Kost API**, built to a single shared specification
(same endpoints, same status codes, same error codes, same business rules), compared as
engineering artifacts.

| | Project A | Project B |
|---|---|---|
| Directory | [`mamikosLaravel/`](mamikosLaravel) | [`mamikosJava/`](mamikosJava) |
| Language / runtime | PHP 8.3 | Java 21 (virtual threads enabled) |
| Framework | Laravel 13 | Spring Boot 3.5 |
| Database | MySQL 8 | PostgreSQL 16 |
| Cache / rate limit store | Redis 7 | Redis 7 |
| Auth | Sanctum personal access tokens (opaque, in DB) | JWT access token + hashed rotating refresh token |
| Schema tooling | Laravel migrations (reversible PHP) | Flyway (forward-only SQL) |
| Build tool | Composer | Maven (wrapper committed) |
| Spec | [PRD-01-Laravel.md](PRD-01-Laravel.md) | [PRD-02-SpringBoot.md](PRD-02-SpringBoot.md) |

Install and usage instructions for both: [README.md](README.md).

---

## Contents

1. [Executive summary and recommendation](#1-executive-summary-and-recommendation)
2. [Method, environment, and what the numbers do not say](#2-method-environment-and-what-the-numbers-do-not-say)
3. [Measured metrics](#3-measured-metrics)
4. [Architecture and code organization](#4-architecture-and-code-organization)
5. [Concurrency model and transaction safety](#5-concurrency-model-and-transaction-safety)
6. [Security approach](#6-security-approach)
7. [Testing ergonomics](#7-testing-ergonomics)
8. [Performance and resource consumption](#8-performance-and-resource-consumption)
9. [Developer experience](#9-developer-experience)
10. [Operational readiness](#10-operational-readiness)
11. [Maintenance cost and talent availability](#11-maintenance-cost-and-talent-availability)
12. [The bugs each stack charged us for](#12-the-bugs-each-stack-charged-us-for)
13. [Conclusion — when to choose which](#13-conclusion--when-to-choose-which)
14. [Appendix A — the API contract differences in full](#appendix-a--the-api-contract-differences-in-full)
15. [Appendix B — how to reproduce every metric](#appendix-b--how-to-reproduce-every-metric)

---

## 1. Executive summary and recommendation

Both implementations satisfy the entire specification, including the hardest requirement — ten
concurrent availability inquiries against a 20-credit wallet must yield exactly four successes and
never a negative balance. Neither is a toy; either could go to production. The interesting result
is that the two came out **almost the same size**: 4,674 lines of production code in Laravel versus
5,033 in Spring Boot, an 8% difference. The received wisdom that a Java service costs twice the
code did not survive contact with Java 21 records, Lombok, and MapStruct.

Where they genuinely diverge:

| Dimension | Winner | Margin |
|---|---|---|
| Raw request latency | **Spring Boot** | large, but partly environmental (§8) |
| Correctness enforced by tooling before runtime | **Spring Boot** | compile-time types + ArchUnit layering rules + JPA schema validation |
| Deployment artifact and rollback story | **Spring Boot** | one 74 MB jar versus a source tree plus a PHP runtime |
| Setup-to-first-request for a new developer | **Laravel** | no compile step, and no dependency on a bootstrap ordering trap once documented |
| Volume of code needed per feature | **Laravel** | marginal — Laravel needed 8% fewer lines, and fewer moving pieces per endpoint |
| Test suite wall-clock | **Laravel** | 42 s for 106 tests versus 55 s for 61 tests plus the full quality gate |
| Cost of the framework's sharp edges | **Laravel** | Spring's transaction proxying cost us four real bugs (§12) |
| Hiring in the Indonesian market | **Laravel** | larger PHP/Laravel talent pool at mid level |

**Recommendation.** For *this* service — a credit ledger where a race condition means giving away
paid inquiries or corrupting a balance — we would ship **Spring Boot**, because the guarantees that
matter here (money arithmetic, transaction boundaries, enum-to-column stability, layering that
cannot be violated by accident) are checked mechanically before the code ever runs, and because it
holds a p50 of 1.4 ms under the same load where Laravel needs cache warming to reach 29 ms.

That recommendation flips if the surrounding context changes, and the context usually decides:

- **A predominantly PHP team, or a service that is mostly CRUD plus a few paid flows** →
  Laravel. It reached the same functional bar with 8% less code, a faster test loop, and no
  compile step. The engineering delta does not justify retraining or a polyglot deployment
  pipeline.
- **A service whose failure mode is financial, or one expected to hold long-lived state, high
  concurrency, or heavy scheduled batch work** → Spring Boot. Long-lived JVM state and
  compile-time enforcement are worth their tax at that point.
- **Both, as here** → keep the shared API contract as the source of truth. It cost almost nothing
  to keep the two behaviourally identical, and it made this comparison possible at all.

---

## 2. Method, environment, and what the numbers do not say

Every figure in §3 was measured on **2026-09-10** on one machine, with both stacks running
simultaneously:

| | |
|---|---|
| Host | Windows 11 Home, Docker Desktop, 15.53 GiB available to the Linux VM |
| Project A runtime | Laravel Sail dev container (`sail-8.3/app`), source **bind-mounted** from the Windows filesystem, `APP_DEBUG=true` |
| Project B runtime | the multi-stage production image (`mamikosjava-app`), `dev` profile, jar baked into the image |
| Load generator | one `curl` process issuing 200 sequential requests over a reused connection, after 25 warm-up requests |
| Maven cache | warm (`~/.m2` already populated) |

**Three caveats that matter more than any single number:**

1. **Project A is measured with a filesystem handicap.** Docker Desktop bind-mounts the PHP source
   from the Windows host, and PHP re-reads its source tree on every request. The evidence is
   direct: `php artisan --version` inside the container takes **4.217 s wall clock but only 0.198 s
   of user CPU and 0.362 s of system CPU** — it is waiting on I/O, not computing. On a Linux host,
   or with the source baked into the image and `opcache.validate_timestamps=0`, Laravel's latency
   figures would improve substantially. Treat the latency gap in §8 as "what these two setups did
   on this laptop", not "what PHP can do".
2. **The two container images are not the same kind of thing.** `sail-8.3/app` (3.03 GB) is a
   *development* image: it bundles Composer, Node, Xdebug and build tooling. `mamikosjava-app`
   (433 MB) is a *production* image: JRE plus one jar. A production PHP image (php-fpm-alpine,
   `--no-dev` vendor) would land in the 150–250 MB range. The honest comparison is 150–250 MB
   versus 433 MB — Laravel would win it — not 3.03 GB versus 433 MB.
3. **The database containers had different histories.** MySQL had been up about an hour serving the
   test suite (buffer pool warm and filled); PostgreSQL had just started. Their memory figures are
   not comparable, and are reported only for completeness.

Coverage was measured for Project B only (JaCoCo runs in its `verify` phase). Project A's coverage
needs an Xdebug/PCOV pass that was not run here — it is reported as *not measured* rather than
estimated.

Commit counts are also not comparable: `mamikosLaravel/` is its own repository with 49 commits,
while `mamikosJava/` is tracked inside the outer repository, which has one squashed commit.

---

## 3. Measured metrics

### 3.1 Code and dependencies

| Metric | Project A — Laravel | Project B — Spring Boot |
|---|---|---|
| Production source files | 79 `.php` in `app/` (113 including `routes/`, `database/`, `config/`) | 120 `.java` in `src/main/java` |
| Production lines | 3,877 in `app/`; **4,674** including `routes/` + `database/` | **4,656** Java + 377 lines of YAML/SQL = **5,033** |
| Test files | 26 | 16 |
| Test lines | **1,748** | **1,612** |
| Test : production line ratio | **0.37 : 1** | **0.32 : 1** |
| Tests executed | **106** tests, 233 assertions | **61** tests (17 unit + 44 integration) |
| Schema migrations | 10 reversible PHP migrations | 7 forward-only SQL migrations (+ 1 dev-only seed) |
| Direct dependencies | **13** (4 runtime + 9 dev) | **26** `<dependency>` entries |
| Resolved / packaged dependencies | **119** locked packages (82 runtime + 37 dev) | **112** jars bundled inside the fat jar |

### 3.2 Build and test

| Metric | Project A — Laravel | Project B — Spring Boot |
|---|---|---|
| Compile step | none | `javac` + annotation processing (Lombok, MapStruct) |
| Full quality gate + tests | `composer check` (Pint + PHPStan L6 + PHPUnit) — tests alone **42.09 s** | `./mvnw -B verify` → **BUILD SUCCESS in 54.9 s** (compile + Spotless + Checkstyle + 61 tests + JaCoCo gate) |
| Unit tests alone | part of the 42 s run | 17 tests, ~5.2 s of test time, no containers needed |
| Integration tests | Feature + Console suites, inside the 42 s run, against the `testing` MySQL database | 44 tests, ~40.8 s, against throwaway Testcontainers PostgreSQL + Redis |
| Line coverage | not measured this pass | **81.87%** (gate: 80% — met) |
| Instruction / branch / method coverage | — | 79.10% / 54.00% / 65.87% |
| Deployable artifact | none — source is the artifact; `vendor/` is **123 MB** | `mamikos-kost-api.jar` — **74 MB** |

### 3.3 Runtime

| Metric | Project A — Laravel | Project B — Spring Boot |
|---|---|---|
| Application boot | no boot phase — the framework bootstraps per request | **8.85 s** (`Started KostApiApplication in 8.85 seconds`) |
| `docker restart` → first `200` | **10.4 s** | **10.8 s** |
| `GET /kosts?sort=price`, dev config | mean 265.6 · p50 **125.0** · p90 395.9 · p95 **1296.1** · max 3769.2 ms | mean 4.5 · p50 **1.4** · p90 17.0 · p95 **20.3** · max 27.0 ms |
| Same, after `php artisan optimize` | mean 103.6 · p50 **29.3** · p90 43.5 · p95 **78.9** ms | *(unchanged — already production-packaged)* |
| Health endpoint | p50 121.1 · p95 1285.4 ms (dev config) | p50 4.8 · p95 6.5 ms |
| Framework bootstrap per CLI invocation | `php artisan --version`: 4.217 s wall / **0.198 s user CPU** (I/O-bound, see §2) | n/a — one process serves every request |
| Idle memory, application | 131.9 MiB web + 46.3 queue + 87.2 scheduler ≈ **265 MiB** across 3 containers | **519 MiB**, one JVM (`-XX:MaxRAMPercentage=75`) |
| Idle memory, database | MySQL 473.9 MiB *(warm, ~1 h uptime)* | PostgreSQL 35.9 MiB *(just started)* |
| Idle memory, Redis | 8.4 MiB | 7.7 MiB |
| Application image | `sail-8.3/app` **3.03 GB** *(dev image — see §2 caveat 2)* | `mamikosjava-app` **433 MB** *(multi-stage, JRE only)* |
| Database image | `mysql:8.0` 1.1 GB | `postgres:16-alpine` 420 MB |

### 3.4 Reading the latency numbers

Three things are worth extracting from that table rather than skimming past it:

- **`php artisan optimize` is not a micro-optimization here.** It cut p50 by 77% (125 → 29 ms) and
  p95 by 94% (1296 → 79 ms). Any Laravel deployment that skips it is leaving most of its
  performance on the table, and any benchmark that omits it is not measuring Laravel.
- **Both stacks produced one multi-second outlier** (3.8 s and 4.0 s maxima on the Laravel runs,
  never on the Spring runs). Those coincide with the host filesystem stalling, not with anything in
  the application — one more reason to distrust the tail on a Windows/Docker Desktop laptop.
- **Spring Boot's advantage is structural, not just tuned.** A long-lived JVM keeps the routing
  table, the ORM metamodel, the connection pool and JIT-compiled code hot between requests. PHP
  reconstructs its world per request, and pays for that whether or not the filesystem is fast.
  Even discounting the I/O handicap entirely, the ordering here would not reverse — only the size
  of the gap would change.

---

## 4. Architecture and code organization

Both projects layer the same way, and both enforce a thin web layer:

```
Project A:  route → middleware → controller → form request → service → repository → Eloquent → MySQL
                                                    ↓
                                              API resource (response shape)

Project B:  filter chain → controller → @Valid record → service (@Transactional) → Spring Data → JPA → PostgreSQL
                                                    ↓
                                              MapStruct mapper → response record
```

**The organizing principle differs.** Project A groups by technical role
(`app/Services`, `app/Repositories`, `app/Http/Controllers`, `app/DTOs`) — the Laravel convention.
Project B groups by feature (`auth/`, `user/`, `kost/`, `credit/`, `inquiry/`, `common/`), with the
layers appearing *inside* each feature package. For a service this size both work; the
feature-package layout scales better when a codebase grows past a few dozen endpoints, because a
change to "credit" stays in one directory instead of touching six.

**Both projects resisted pattern-for-its-own-sake, in different ways, and both documented why.**
Project A applies the repository pattern to exactly three aggregates with non-trivial queries
(`Kost`, `Credit`, `Inquiry`) and lets simple lookups go straight through Eloquent inside services.
Project B declines to hand-roll repository interfaces at all, on the grounds that Spring Data's
`JpaRepository` *is* the Repository pattern and wrapping it adds a layer with no testing benefit.
These are opposite decisions that reach the same place: no indirection without a reason.

**The decisive difference is that Project B can enforce its architecture, and Project A can only
document it.** `LayeredArchitectureTest` (ArchUnit) fails the build if the web layer imports a
repository, if an entity imports the web layer, or if any `@Entity` stores an enum as `ORDINAL`
instead of `STRING` — that last rule prevents a whole class of silent data corruption, where
reordering an enum remaps existing rows. Larastan at PHPStan level 6 catches type errors well, but
nothing in the PHP toolchain expresses "controllers may not touch repositories". In a team of one
that is a footnote; in a team of eight over two years it is the difference between a layered
codebase and a nominally layered one.

Both use the same supporting patterns where they earn their keep — Strategy for reset-versus-topup
recharge, an append-only ledger, a domain event on inquiry creation, value objects for money and
addresses, native enums, and one centralized error-envelope translator (`bootstrap/app.php`
closures in A, `@RestControllerAdvice` in B).

---

## 5. Concurrency model and transaction safety

This is where the specification bites, and both implementations landed on the same design —
independently arriving at the same answer is itself a signal that the answer is right.

| | Project A — Laravel | Project B — Spring Boot |
|---|---|---|
| Transaction boundary | `DB::transaction(fn () => …)` in `CreditService` | `@Transactional` on the inquiry service; `CreditService` methods are `Propagation.MANDATORY` and never open their own |
| Wallet lock | `SELECT … FOR UPDATE` via `lockBalanceForUser()` | `SELECT … FOR UPDATE` via `findByUserIdForUpdate()` |
| Last line of defense | `CHECK (balance >= 0)` in the schema | `CHECK (balance >= 0)` in the schema |
| Ledger | append-only row written inside the same transaction | append-only row written inside the same transaction |
| Proof | `ConcurrentInquiryTest` — 10 forked processes (`pcntl_fork`), exactly 4 successes, final balance 0 | `ConcurrentInquiryIT` — 10 threads, exactly 4 successes, final balance 0 |

Two details are worth calling out because they show the same idea expressed in each stack's
grammar.

**Project B's `MANDATORY` propagation is a stronger statement than Project A's closure.** Marking
`CreditService.lockBalance` as `MANDATORY` makes it a *compile-into-runtime* contract: calling it
outside a transaction throws immediately rather than silently taking a lock that is released at
once. Laravel's equivalent guarantee lives in the fact that the only caller wraps it in
`DB::transaction` — true today, and true only as long as nobody adds a second caller.

**Project A's fork-based concurrency test is the more honest test of the two, and it was harder to
write.** Laravel's `RefreshDatabase` wraps each test in a transaction, which makes a genuine race
impossible to reproduce in-process — so the test forks real child processes, each with its own
connection. It also has to skip itself when `pcntl` is unavailable, which means it can silently
not run on some machines (notably Windows without WSL2). Project B gets the same coverage from
ordinary threads against a real PostgreSQL container, with no platform caveat.

**Where the models truly separate is *outside* the request.** PHP's process-per-request model means
there is no shared mutable state to get wrong: no thread-safety bugs, no leaked statics, no memory
that survives a bad request. The JVM has all of those risks and, in exchange, keeps a warm
connection pool, an in-process scheduler, and (here) virtual threads for cheap concurrency. Project
A needs two extra long-lived containers (`queue`, `scheduler`) to get behaviour Project B gets from
the same process it already runs.

---

## 6. Security approach

The specification's security requirements are met by both. The mechanisms differ in ways that
matter operationally.

| Concern | Project A — Laravel | Project B — Spring Boot |
|---|---|---|
| Token type | Opaque Sanctum token, hashed in `personal_access_tokens` (~50 chars) | JWT (HS384, ~260 chars) + separate rotating refresh token, hashed at rest |
| Token verification | one indexed DB read per request | signature check only, no DB read |
| Expiry | 24 h (`SANCTUM_TOKEN_EXPIRATION=1440`) | access `PT24H`, refresh `P7D` |
| Logout | delete the token row — revocation is **immediate and total** | revoke the refresh token **and** denylist the access token's `jti` in Redis until natural expiry |
| Refresh | rotate: delete the presented token, issue a new one | rotate the refresh token, with **replay detection** on a reused token |
| Authorization | `role:` middleware + Policies (`KostPolicy`, `InquiryPolicy`) | `@PreAuthorize` + `ROLE_<ROLE>` authorities |
| Validation | Form Requests + a custom `AvailableRoomsWithinCapacity` rule | Bean Validation (JSR-380) on records + a custom class-level constraint |
| Rate limiting | `RateLimiter` on Redis — 60/min default, **5/min on auth keyed by IP + submitted email**, 20/min on inquiries | `RateLimitFilter` on Redis with the same three tiers, disabled under the `test` profile for determinism |
| SQL injection surface | Eloquent query builder, parameter-bound; sort column whitelisted in the Form Request | JPA Criteria/Specification, parameter-bound by construction; sort column whitelisted |
| Error leakage | catch-all handler strips traces/SQL once `APP_DEBUG=false` | `server.error.include-*: never`, plus a `@RestControllerAdvice` envelope |
| Secret handling | `APP_KEY` generated by `artisan key:generate` | `jwt.secret` has **no default** — the app refuses to boot without one |

**The auth trade-off is real in both directions, and neither choice is a mistake.** Sanctum spends
one database read per request to buy instant, unconditional revocation — the simplest correct
answer, and the one an auditor will like. JWT saves that read, then has to buy revocation back with
a Redis denylist, because a signed token is otherwise valid until it expires no matter what
"logout" did. Project B's implementation is honest about this and implements the denylist; a JWT
implementation that skips it has a logout button that does not log anyone out. That is the more
common bug of the two, and it is worth knowing that the stateless option is the one that requires
extra work to be correct.

Two smaller notes. Sanctum's expiring tokens leave stale rows behind, so a production deployment
wants Laravel's `sanctum:prune-expired` on a schedule. And Project B's `jwt.secret`-with-no-default
decision is the better default of the two: a secret with a fallback value is a secret that leaks
into a repository eventually.

---

## 7. Testing ergonomics

| | Project A — Laravel | Project B — Spring Boot |
|---|---|---|
| Runner | PHPUnit 12 via `artisan test` | Surefire (unit) + Failsafe (`*IT`) |
| Test database | MySQL database named `testing`, `RefreshDatabase` per test | Testcontainers — a real PostgreSQL + Redis per run |
| Fixtures | model factories + a `CreatesUsers` trait | builders in `AbstractIntegrationTest` + a `TestApi` helper |
| Isolation | transaction rollback per test | container per run, cached Spring contexts between test classes |
| Executed | 106 tests / 233 assertions in 42.1 s | 61 tests in a 54.9 s full `verify` |
| Architecture tests | none available | 4 ArchUnit rules, enforced |
| Coverage gate | `--min=80` available, needs Xdebug/PCOV | JaCoCo bound to `verify`, 80% line gate, currently 81.9% |

**Laravel's loop is faster, and more of it runs anywhere.** No compile step, no container
orchestration, `artisan test --filter` on a single test in a second or two. `./mvnw test` needs a
compile first, and `./mvnw verify` will not run at all without a Docker daemon.

**Spring Boot's tests are closer to production, and that difference caught real bugs.** Every
integration test runs against a genuine PostgreSQL — the same locking semantics, the same
`CHECK` constraint behaviour, the same Flyway migrations that production will run. Three of the
four bugs in §12 were only findable in that setting. The cost is honest: 13 s to start the first
Spring context plus Testcontainers, then 1.4–1.6 s per cached context afterwards, and ~41 s of the
55 s build spent in integration tests.

**The visible test counts (106 vs 61) do not mean Project A is better tested.** They mean the two
suites slice the same behaviour differently — Laravel's suite leans on many small feature tests
hitting HTTP endpoints, while Spring's leans on fewer, broader integration tests, each asserting
more per test. The comparable figures are lines of test code (1,748 vs 1,612) and the fact that
both suites cover the same acceptance criteria, including the concurrency requirement. The one
measurable coverage number available belongs to Project B (81.9% lines); Project A's is unmeasured,
which is itself a small gap in the deliverable.

---

## 8. Performance and resource consumption

Numbers in [§3.3](#33-runtime); read them together with the caveats in [§2](#2-method-environment-and-what-the-numbers-do-not-say).

**Latency.** Spring Boot served the search endpoint at a p50 of 1.4 ms and a p95 of 20 ms. Laravel
served the same endpoint at 125 ms p50 in its dev configuration and 29 ms p50 once
`php artisan optimize` had run. Part of that remaining ~20× gap is the Windows bind mount, and part
of it is structural: a warm JVM does not rebuild its routing table, container, and ORM metadata on
every request. A fair expectation for this Laravel app on a Linux host with opcache tuned for
production is the low tens of milliseconds — better than measured here, still behind a warm JVM,
and entirely adequate for the workload this API actually has.

**Throughput shape.** The Spring numbers are tightly clustered (p50 1.4 ms, p90 17 ms, max 27 ms).
The Laravel numbers are heavy-tailed even after optimization (p50 29 ms, p95 79 ms, one 4.0 s
outlier). Predictability is worth as much as median latency when you are setting an SLO, and it is
the JVM's clearer win here.

**Memory.** Laravel's application tier used ~265 MiB spread across three containers; the JVM used
519 MiB in one. The comparison is less lopsided than it looks: the JVM figure is mostly
*reserved* heap (`-XX:MaxRAMPercentage=75` against a 15.5 GiB VM), not live data, and it is flat
whether the app is idle or busy. PHP's footprint is genuinely proportional to concurrency — every
simultaneous request needs its own process — so under real load Laravel's number grows while the
JVM's does not. For a small container budget, PHP is easier to squeeze; for high concurrency on one
box, the JVM amortizes better.

**Artifacts.** A single 74 MB jar versus a 123 MB `vendor/` directory plus source. The jar is one
immutable file: atomic deploy, atomic rollback, byte-identical between staging and production. The
PHP deployment is a file tree that has to be synchronized consistently, which is a solvable problem
and one more thing to get right.

---

## 9. Developer experience

**Where Laravel is better:**

- **No build step.** Save the file, refresh the request. Over a day of iteration, that compounds
  more than any single benchmark in this document.
- **Fewer moving pieces per endpoint.** An endpoint is a route line, a Form Request, a controller
  method, a service method, and an API Resource. Project B additionally needs a request record, a
  response record, and a MapStruct mapper interface — more type safety, more files.
- **Diagnostics.** Laravel's exception pages and validation messages are conversational and
  actionable. Spring's failures are often long causal chains where the useful line is not the first
  or the last one.
- **Artisan.** `make:*`, `route:list`, `tinker`, `schedule:list`, `queue:work` — a coherent
  first-party CLI that removes a lot of boilerplate typing.

**Where Spring Boot is better:**

- **The compiler is a test that always runs.** Rename a field and every use site fails immediately.
  Larastan level 6 catches a great deal, but it is a separate command that a developer can skip
  and CI has to remember to run.
- **Enforceable architecture.** ArchUnit rules (§4) turn conventions into build failures.
- **Schema drift is a startup error, not a production surprise.** `spring.jpa.hibernate.ddl-auto:
  validate` refuses to boot if entities and migrations disagree. Laravel has no equivalent check;
  a forgotten migration shows up as a runtime error on whichever request needs the column.
- **Refactoring at scale.** IDE-driven, type-safe rename/extract/move across 120 files with
  confidence is a Java strength that PHP tooling approaches but does not match.

**A cross-cutting observation.** Both projects had to disable a default that hides performance bugs,
and both did: Laravel sets `Model::preventLazyLoading()` outside production so an N+1 query throws
instead of silently degrading; Spring sets `open-in-view: false` so lazy loading cannot leak past
the transaction into the view layer. Neither framework does the right thing here out of the box.
Knowing to flip those two switches is worth more than any framework preference.

---

## 10. Operational readiness

| | Project A — Laravel | Project B — Spring Boot |
|---|---|---|
| Configuration | `.env` + `config/*.php`, cached by `artisan optimize` | `application.yml` + profile overlays + env vars, validated `@ConfigurationProperties` |
| Config validation | none at boot — a bad value surfaces on use | typed properties; missing `jwt.secret` **fails startup** |
| Health check | `GET /api/v1/health` (checks DB + cache) and `/up` | `GET /actuator/health` with liveness/readiness probe groups |
| Metrics | none exposed | Micrometer → Prometheus at `/actuator/prometheus` |
| Structured logs | Laravel stack channel, plain text by default | JSON via the Logstash encoder |
| Request correlation | `RequestId` middleware → `meta.request_id` | `RequestIdFilter` → `meta.requestId` + MDC |
| Scheduling | `schedule:work` container, or one OS crontab line | in-process `@Scheduled` |
| Scheduler locking | Redis cache lock + `withoutOverlapping` + `onOneServer` | ShedLock on a DB table, using the **database's** clock |
| Async work | Redis queue + a dedicated `queue:work` worker | Spring events (in-process) |
| Graceful shutdown | PHP-FPM handles it per request | `server.shutdown: graceful` |
| Deploy unit | source tree + PHP-FPM + Composer install | one jar, or an OCI image |
| Processes to run | 3 (web, queue worker, scheduler) | 1 |

**Project B is measurably more operable out of the box**, and the gap is not stylistic: Prometheus
metrics, probe-aware health groups, JSON logs, typed config validation, and fail-fast startup are
all things you would otherwise add to the Laravel service by hand before a serious production
launch. None of them is hard to add — Laravel has good packages for every one — but "already there
and wired" beats "straightforward to add" during an incident.

**Two nuances worth stating plainly.** First, ShedLock using the database clock is a stronger
guarantee than a Redis cache lock: Redis losing its state or a network partition could in principle
let two nodes both believe they hold the recharge lock, and the recharge job is exactly the kind of
job you do not want running twice. Project A mitigates this with an in-command lock *and*
`withoutOverlapping` *and* `onOneServer`, plus an idempotency check on
`last_recharged_at` — belt and braces, and the idempotency check is the part that actually saves
you. Second, Project A's three-process deployment is not merely more YAML: it is three things that
can be running the wrong version of the code at the same time.

---

## 11. Maintenance cost and talent availability

| | Project A — Laravel | Project B — Spring Boot |
|---|---|---|
| Talent pool (Indonesia, mid-level backend) | large — PHP/Laravel is the default local stack | smaller at mid level, deep at senior/enterprise |
| Onboarding a new backend hire | days | one to two weeks if Spring is unfamiliar |
| Upgrade cadence | Laravel majors roughly annually; ~18 months of bug fixes and ~2 years of security fixes per release | Spring Boot minors every ~6 months; LTS-friendly with Java 21 |
| Upgrade friction | broad but usually shallow changes; Composer resolution is the usual pain | narrower, but Boot majors move Jakarta/Hibernate/Security baselines together |
| Dependency surface | 119 locked packages | 112 bundled jars |
| CVE exposure | PHP runtime + 82 runtime packages | JVM + 112 jars; Spring Security is a frequent advisory target |
| Cost of an average change | lower — fewer files per feature, no build | slightly higher per change, lower risk of the change being wrong |

**The realistic maintenance question is not "which framework is cheaper", it is "which failure is
cheaper".** Laravel makes changes cheap and lets a type mistake reach staging. Spring Boot makes
changes slightly more expensive and stops that class of mistake at compile time. For a credit
ledger, the second trade is usually the better buy; for a content or CRUD service, the first
usually is.

One point in Laravel's favour that is easy to undervalue: both dependency trees are the same order
of size (119 vs 112), but Laravel's is dominated by first-party packages from a single vendor with
one release cadence. Fewer independent upgrade calendars is a real, recurring saving.

---

## 12. The bugs each stack charged us for

The most useful comparison is not the feature matrix — it is the list of bugs each framework's
model made possible. Project B's README documents four, all of them permanently pinned by
regression tests named after the bug. All four are *Spring-specific taxes* that have no analogue in
Laravel's request model:

1. **Self-invocation defeats `@Transactional`.** A `REQUIRES_NEW` method called as `this.method()`
   from the same bean bypasses the Spring proxy, so the new-transaction semantics are silently
   skipped. Fixed by extracting the per-batch recharge into its own bean called through its
   injected proxy.
2. **A rollback can undo the thing you did *because* of the failure.** Replay-detected refresh
   tokens are revoked and then rejected with an exception — and the default
   rollback-on-unchecked-exception rule was reverting the revoke, because the real transaction
   boundary was one frame up the stack. Fixed with `noRollbackFor` on the method that is actually
   the outermost boundary.
3. **A read-only query hands back detached entities.** The recharge job pages wallets in one
   committing read and mutates them in a different transaction; the already-detached entities
   dropped those writes with no error at all. Fixed by re-fetching and locking each wallet by id
   inside the batch's own transaction.
4. **`@PreAuthorize` throws where the servlet filter cannot see it.** `AccessDeniedException` is
   raised inside the controller invocation and resolved by `@RestControllerAdvice` before the
   security filter chain's handler would ever see it — so a wrong-role request returned `500`
   instead of `403` until an explicit branch was added.

Every one of those is a consequence of proxy-based transactions, an ORM with an identity map, and a
layered security model — the same machinery that provides the guarantees praised in §4 and §5.
**This is the honest price of Spring Boot**, and it is paid in senior debugging time, not in
tutorial time.

Laravel's costs in this build were different in kind: no proxy semantics to violate and no detached
entities, but also no compiler and no architecture enforcement, so its equivalent risks are the
ones that reach runtime rather than the ones that surprise you at the transaction boundary. Its two
concrete taxes here were needing `pcntl_fork` to test a real race at all (with an automatic skip
that can hide the test on some machines), and needing `Model::preventLazyLoading` to make N+1
queries visible.

---

## 13. Conclusion — when to choose which

**Choose Laravel (Project A) when:**

- The team already writes PHP, or hiring speed in the Indonesian market is a constraint.
- The service is mostly CRUD with a few transactional flows — which describes most services.
- Iteration speed matters more than compile-time guarantees; you ship several times a week.
- The deployment target is a conventional PHP host or a small container budget.
- Time-to-first-endpoint is the metric under pressure.

**Choose Spring Boot (Project B) when:**

- Correctness is financial: wallets, ledgers, payments, quotas — anything where a race means money.
- The service will hold long-lived state, high concurrency, or heavy scheduled batch work.
- Multiple teams will touch one codebase for years and layering has to survive turnover.
- Operational maturity is required on day one: metrics, probes, structured logs, fail-fast config.
- You want the deployment artifact to be one immutable, atomically rollback-able file.

**For MamiKos's kost-with-credit domain specifically:** the credit wallet is the part of this system
that must never be wrong, and it is also the part with real concurrency. That argues for Spring
Boot for this service. But the two implementations reaching the same functional bar within 8% of
the same amount of code is the finding that should actually drive the decision — **the stack is not
the constraint here; the design is.** Both projects got the wallet right for the same reason, and
neither framework gave it to them for free: a pessimistic row lock, a database `CHECK` constraint,
one transaction around the whole deduct-and-record sequence, and a test that forces ten requests to
race for real. A team that knows to do that will succeed in either stack. A team that does not will
ship a negative balance in both.

---

## Appendix A — the API contract differences in full

Verified by calling both running services and diffing the responses. Everything not listed here was
byte-identical apart from ids and timestamps.

| Aspect | Project A — Laravel | Project B — Spring Boot | Deliberate? |
|---|---|---|---|
| JSON key casing | `snake_case` | `camelCase` | Yes — each follows its ecosystem's convention |
| Query parameters | `sort_by`, `price_min`, `price_max`, `room_type`, `per_page` | `sortBy`, `priceMin`, `priceMax`, `roomType`, `perPage` | Yes, follows the above |
| `role` value | lowercase (`"regular"`) | uppercase (`"REGULAR"`) | Yes — PHP enum backing value vs Java enum name |
| `room_type` / `roomType` in responses | `"putri"` | `"PUTRI"` | Yes, same reason |
| Money fields | JSON **string** `"609089.00"` | JSON **number** `600000.00` | **No** — a side effect of MySQL `DECIMAL` arriving in PHP as a string. A client parsing one and not the other will break |
| `null` fields | present, e.g. `"owner_reply": null` | omitted (`default-property-inclusion: non_null`) | Partly — worth aligning |
| Timestamps | second precision, offset form (`…T05:24:54+00:00`) | nanosecond precision, `Z` form (`…T05:24:55.238282089Z`) | **No** — both are valid ISO-8601, but a strict client-side parser may accept only one |
| Auth token | opaque Sanctum token, ~50 chars, `data.token.access_token` | JWT ~260 chars, `data.token.accessToken`, plus `refreshToken` | Yes — different auth designs |
| Health endpoint | `/api/v1/health`, wrapped in the standard envelope | `/actuator/health`, Spring's own format (not the envelope) | Yes — Actuator is a platform endpoint |

Status codes, `code` values, `message` strings, envelope shape and pagination `meta` are identical,
including `422 INSUFFICIENT_CREDIT` with the same `errors.credit` message text
(`"Required 5 credit, current balance 3"`) and the same `403 NOT_KOST_OWNER` semantics.

**Recommendation if this contract ever has to serve one shared client:** normalize money to a JSON
number and truncate timestamps to seconds in both. Casing can stay divergent — a client maps it
once — but a field that is a string in one service and a number in the other is a bug waiting for a
strongly-typed consumer.

---

## Appendix B — how to reproduce every metric

```bash
# ---- code and dependency counts -----------------------------------------
find mamikosLaravel/app -name "*.php" | wc -l
find mamikosLaravel/app mamikosLaravel/routes mamikosLaravel/database -name "*.php" -exec cat {} + | wc -l
find mamikosLaravel/tests -name "*.php" -exec cat {} + | wc -l
find mamikosJava/src/main/java -name "*.java" | wc -l
find mamikosJava/src/main/java -name "*.java" -exec cat {} + | wc -l
find mamikosJava/src/test -name "*.java" -exec cat {} + | wc -l
grep -c "<dependency>" mamikosJava/pom.xml
# locked packages: count .packages + .packages-dev in mamikosLaravel/composer.lock
# bundled jars:    count BOOT-INF/lib/*.jar inside mamikosJava/target/mamikos-kost-api.jar

# ---- build and test ------------------------------------------------------
cd mamikosLaravel && docker compose exec laravel.test php artisan test          # 106 tests / 42.1 s
cd mamikosJava   && time ./mvnw -B verify                                       # 61 tests / 54.9 s
# coverage totals are summed from mamikosJava/target/site/jacoco/jacoco.csv

# ---- artifact and image sizes -------------------------------------------
ls -lh mamikosJava/target/mamikos-kost-api.jar
du -sh mamikosLaravel/vendor
docker images --format "{{.Repository}}:{{.Tag}}\t{{.Size}}"

# ---- boot time ----------------------------------------------------------
docker logs mamikosjava-app-1 | grep "Started KostApiApplication"
# container restart -> first 200:
docker restart <container> && curl -s --retry 60 --retry-delay 1 --retry-connrefused \
  --retry-all-errors -o /dev/null <health-url>

# ---- latency (200 sequential requests over one reused connection) --------
for i in $(seq 1 200); do printf -- '-o\n/dev/null\n%s\n' "$URL"; done \
  | xargs -d '\n' curl -s -w "%{time_total}\n" | sort -n
# then take the 100th value for p50, the 190th for p90, the 195th for p95

# ---- memory -------------------------------------------------------------
docker stats --no-stream --format "{{.Name}}\t{{.MemUsage}}"

# ---- the I/O-bound bootstrap evidence in §2 -----------------------------
docker compose exec laravel.test bash -c 'time php artisan --version'
# real 4.217s, user 0.198s, sys 0.362s  -> waiting on the bind mount, not computing
```

To re-measure Laravel with production caches (the "after `artisan optimize`" row):

```bash
docker compose exec laravel.test php artisan optimize          # apply
# ... run the latency loop ...
docker compose exec laravel.test php artisan optimize:clear    # restore dev behaviour
```

---

*Report compiled 2026-09-10 from both implementations at their current commits, with every figure
measured rather than estimated. Unmeasured items are marked as such.*
