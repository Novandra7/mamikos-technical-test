# MamiKos Kost API — Spring Boot Edition

Backend API for MamiKos: owners list kosts, users search them and pay a small amount of
credit to ask about real room availability. Built to the [MamiKos Backend Technical
Test](../PRD-02-SpringBoot.md) specification — see that document for the full product
requirements; this README covers installing, building, and using what was actually built.

**Stack:** Java 21 · Spring Boot 3.5 · PostgreSQL 16 · Redis 7 · Maven · Flyway · JWT

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Quick Start — Docker Compose (recommended)](#2-quick-start--docker-compose-recommended)
3. [Local Install (without Docker)](#3-local-install-without-docker)
4. [Environment Variables](#4-environment-variables)
5. [Running the Test Suite](#5-running-the-test-suite)
6. [Code Quality Checks](#6-code-quality-checks)
7. [API Documentation](#7-api-documentation)
8. [Demo Walkthrough](#8-demo-walkthrough)
9. [Monthly Credit Recharge](#9-monthly-credit-recharge)
10. [Building for Production](#10-building-for-production)
11. [Architecture & Design Decisions](#11-architecture--design-decisions)
12. [Project Structure](#12-project-structure)
13. [Troubleshooting](#13-troubleshooting)

---

## 1. Prerequisites

| Tool | Minimum Version | Check |
|---|---|---|
| JDK | 21 (Temurin/Corretto/Zulu) | `java -version` |
| Docker + Compose v2 | 24+ | `docker compose version` |
| Git | 2.30 | `git --version` |

Maven itself is **not** required — the repo ships the Maven Wrapper (`mvnw` / `mvnw.cmd`),
which downloads the right Maven version on first use.

For the local (non-Docker) path you'll also need:

| Tool | Minimum Version |
|---|---|
| PostgreSQL | 16 |
| Redis | 7 |

---

## 2. Quick Start — Docker Compose (recommended)

```bash
git clone <repository-url> mamikos-kost-api
cd mamikos-kost-api

# Generate a JWT secret and put it where docker-compose can read it
cp .env.example .env
# Windows PowerShell: [Convert]::ToBase64String((1..48 | % { Get-Random -Max 256 }))
# macOS/Linux:        openssl rand -base64 48
# Paste the output as JWT_SECRET= in .env

docker compose up -d --build
```

This starts three containers: `postgres`, `redis`, and `app` (built from the `Dockerfile`,
multi-stage so the final image is JRE-only). Flyway migrates the schema automatically on
first boot, and — because `SPRING_PROFILES_ACTIVE` defaults to `dev` in `docker-compose.yml`
— the demo dataset described in [§8](#8-demo-walkthrough) is seeded too.

**Verify it's up:**
```bash
curl http://localhost:8080/actuator/health
# {"status":"UP","components":{"db":{"status":"UP"},"redis":{"status":"UP"}}}
```

**Useful commands:**
```bash
docker compose logs -f app       # tail application logs
docker compose exec app sh       # shell into the running container
docker compose down -v           # stop and wipe the database volume
```

To run it as it would run in production, set `SPRING_PROFILES_ACTIVE=prod` before `up`
(no demo data, stricter logging) — see [§10](#10-building-for-production).

---

## 3. Local Install (without Docker)

**Step 1 — Clone**
```bash
git clone <repository-url> mamikos-kost-api
cd mamikos-kost-api
```

**Step 2 — Create the database**
```bash
psql -U postgres -c "CREATE USER mamikos WITH PASSWORD 'secret';"
psql -U postgres -c "CREATE DATABASE mamikos_kost OWNER mamikos;"
```

**Step 3 — Export configuration**

`JWT_SECRET` has **no built-in default** in the base config — the app refuses to start
without it, on purpose (a secret with a fallback value is a secret that leaks). The `dev`
profile alone provides a placeholder so local development works out of the box; every other
profile requires you to set it explicitly.

```bash
# Linux/macOS
export DB_URL=jdbc:postgresql://localhost:5432/mamikos_kost
export DB_USERNAME=mamikos
export DB_PASSWORD=secret
export REDIS_HOST=localhost
export JWT_SECRET=$(openssl rand -base64 48)
```
```powershell
# Windows PowerShell
$env:DB_URL="jdbc:postgresql://localhost:5432/mamikos_kost"
$env:DB_USERNAME="mamikos"
$env:DB_PASSWORD="secret"
$env:REDIS_HOST="localhost"
$env:JWT_SECRET=[Convert]::ToBase64String((1..48|%{Get-Random -Max 256}))
```

**Step 4 — Run**
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```
(Windows: `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"`)

The `dev` profile migrates the schema **and** seeds the demo dataset — see
[§8](#8-demo-walkthrough).

**Step 5 — Verify**
```bash
curl http://localhost:8080/actuator/health
```

---

## 4. Environment Variables

| Variable | Default | Required | Notes |
|---|---|:--:|---|
| `SPRING_PROFILES_ACTIVE` | *(none)* | ➖ | `dev` for local work (seeds demo data), `prod` for production, `test` is used automatically by the test suite |
| `DB_URL` | `jdbc:postgresql://localhost:5432/mamikos_kost` | ✅ | |
| `DB_USERNAME` | `mamikos` | ✅ | |
| `DB_PASSWORD` | `secret` | ✅ | |
| `REDIS_HOST` | `localhost` | ✅ | |
| `REDIS_PORT` | `6379` | ➖ | |
| `JWT_SECRET` | *(none — dev profile only)* | ✅ | Base64, ≥ 256 bits. Generate with `openssl rand -base64 48` |
| `JWT_ACCESS_TOKEN_TTL` | `PT24H` | ➖ | ISO-8601 duration |
| `JWT_REFRESH_TOKEN_TTL` | `P7D` | ➖ | |
| `CREDIT_QUOTA_REGULAR` | `20` | ➖ | |
| `CREDIT_QUOTA_PREMIUM` | `40` | ➖ | |
| `CREDIT_INQUIRY_COST` | `5` | ➖ | |
| `CREDIT_RECHARGE_STRATEGY` | `RESET` | ➖ | `RESET` or `TOPUP` |
| `CREDIT_RECHARGE_CRON` | `0 0 0 1 * *` | ➖ | Quartz-style cron, evaluated in `CREDIT_TIMEZONE` |
| `CREDIT_TIMEZONE` | `Asia/Jakarta` | ➖ | |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | ➖ | Comma-separated |
| `RATE_LIMIT_ENABLED` | `true` | ➖ | Disabled automatically under the `test` profile for deterministic tests |

A full reference lives in `.env.example`.

---

## 5. Running the Test Suite

**Docker must be running** — the integration tests use Testcontainers to start real,
throwaway PostgreSQL and Redis instances; nothing here talks to your own local database.

```bash
./mvnw test              # unit tests + ArchUnit rules (fast, no containers needed)
./mvnw verify             # + integration tests + the 80% line-coverage gate
```

Coverage report: `target/site/jacoco/index.html`.

The suite includes a deliberately adversarial concurrency test
(`ConcurrentInquiryIT`): ten simultaneous availability inquiries against a 20-credit wallet
(cost 5 each) must yield **exactly four** successes and leave the balance at **exactly
zero** — proving the pessimistic row lock actually prevents the wallet from going negative
under real parallel load, not just in a single-threaded happy path.

---

## 6. Code Quality Checks

```bash
./mvnw spotless:check      # formatting (palantir-java-format)
./mvnw spotless:apply      # auto-fix formatting
./mvnw checkstyle:check    # style + defect-prone-pattern rules (checkstyle.xml)
```

Both run automatically as part of `./mvnw verify` — a build with unformatted code or a
style violation fails before a single test runs.

---

## 7. API Documentation

With the app running:

| Resource | URL |
|---|---|
| Swagger UI (interactive) | http://localhost:8080/swagger-ui.html |
| OpenAPI 3 spec (JSON) | http://localhost:8080/v3/api-docs |
| Postman collection | [`docs/postman_collection.json`](docs/postman_collection.json) — import, then set the collection variable `baseUrl` (default `http://localhost:8080/api/v1`) |

### Endpoint summary

| Method | Path | Auth | Role |
|---|---|:--:|---|
| POST | `/api/v1/auth/register` | — | — |
| POST | `/api/v1/auth/login` | — | — |
| POST | `/api/v1/auth/refresh` | — | — |
| POST | `/api/v1/auth/logout` | ✅ | any |
| GET | `/api/v1/auth/me` | ✅ | any |
| GET | `/api/v1/kosts` | — | — |
| GET | `/api/v1/kosts/{id}` | — | — |
| POST | `/api/v1/owner/kosts` | ✅ | OWNER |
| GET | `/api/v1/owner/kosts` | ✅ | OWNER |
| GET | `/api/v1/owner/kosts/{id}` | ✅ | OWNER |
| PUT / PATCH | `/api/v1/owner/kosts/{id}` | ✅ | OWNER |
| DELETE | `/api/v1/owner/kosts/{id}` | ✅ | OWNER |
| POST | `/api/v1/kosts/{id}/availability-inquiries` | ✅ | REGULAR/PREMIUM |
| GET | `/api/v1/me/inquiries` | ✅ | REGULAR/PREMIUM |
| GET | `/api/v1/me/credits` | ✅ | REGULAR/PREMIUM |
| GET | `/api/v1/me/credits/transactions` | ✅ | REGULAR/PREMIUM |
| GET | `/api/v1/owner/inquiries` | ✅ | OWNER |
| POST | `/api/v1/owner/inquiries/{id}/reply` | ✅ | OWNER |
| GET | `/actuator/health` | — | — |

Every response — success or error — uses one envelope shape:
```json
{ "success": true,  "message": "...", "data": {}, "meta": { "requestId": "...", "timestamp": "..." } }
{ "success": false, "message": "...", "code": "INSUFFICIENT_CREDIT", "errors": {}, "meta": {} }
```

---

## 8. Demo Walkthrough

The `dev` profile seeds five accounts (password **`Password123`** for all) and thirty kosts
across five cities:

| Account | Email | Role | Starting Balance |
|---|---|---|---|
| Owner 1 | `owner1@mamikos.test` | OWNER | — (no wallet) |
| Owner 2 | `owner2@mamikos.test` | OWNER | — (no wallet) |
| Regular user | `regular@mamikos.test` | REGULAR | 20 |
| Premium user | `premium@mamikos.test` | PREMIUM | 40 |
| Low-credit user | `lowcredit@mamikos.test` | REGULAR | 3 |

### Login and search
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"regular@mamikos.test","password":"Password123"}' \
  | python -c "import json,sys;print(json.load(sys.stdin)['data']['token']['accessToken'])")

# Search by location, sorted by price ascending
curl "http://localhost:8080/api/v1/kosts?location=Yogyakarta&sortBy=price&order=asc"

# Search by price range
curl "http://localhost:8080/api/v1/kosts?priceMin=800000&priceMax=1500000"
```

### Ask about availability (costs 5 credit)
```bash
curl -X POST http://localhost:8080/api/v1/kosts/1/availability-inquiries \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"message":"Masih ada kamar kosong untuk bulan depan?"}'
# -> 201, credit.balanceAfter = 15, availability.availableRooms revealed
```

### Exhaust the low-credit account (proves the 422 rejection)
```bash
LOW_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"lowcredit@mamikos.test","password":"Password123"}' \
  | python -c "import json,sys;print(json.load(sys.stdin)['data']['token']['accessToken'])")

curl -X POST http://localhost:8080/api/v1/kosts/1/availability-inquiries \
  -H "Authorization: Bearer $LOW_TOKEN" -H "Content-Type: application/json" \
  -d '{"message":"test"}'
# -> 422 INSUFFICIENT_CREDIT (balance is 3, cost is 5); balance stays untouched
```

### Owner manages their listings
```bash
OWNER_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"owner1@mamikos.test","password":"Password123"}' \
  | python -c "import json,sys;print(json.load(sys.stdin)['data']['token']['accessToken'])")

curl http://localhost:8080/api/v1/owner/kosts -H "Authorization: Bearer $OWNER_TOKEN"
curl http://localhost:8080/api/v1/owner/inquiries -H "Authorization: Bearer $OWNER_TOKEN"
```

---

## 9. Monthly Credit Recharge

The scheduled job resets every REGULAR/PREMIUM wallet to its role's full quota at
00:00 on the 1st of the month (`Asia/Jakarta` by default). It's also runnable on demand for
verification, without waiting for the schedule:

```bash
# Build the jar first if you haven't:
./mvnw clean package -DskipTests

# See what WOULD happen, without writing anything:
java -jar target/mamikos-kost-api.jar --spring.profiles.active=dev --job=credit-recharge --dry-run

# Actually run it (bypasses the "already recharged this month" check):
java -jar target/mamikos-kost-api.jar --spring.profiles.active=dev --job=credit-recharge --force

# Run it again with no flags — every wallet is now skipped, proving idempotency:
java -jar target/mamikos-kost-api.jar --spring.profiles.active=dev --job=credit-recharge
```

Each invocation prints a summary (`processed`, `recharged`, `skipped`, `failed`) and exits;
it does not start the web server. Additional flags: `--strategy=RESET|TOPUP`,
`--user-id=<id>` to target one wallet.

In a multi-instance deployment, [ShedLock](https://github.com/lukas-krecan/ShedLock) (backed
by a `shedlock` table, using the database's own clock) guarantees only one instance's
`@Scheduled` trigger actually runs the job each month.

---

## 10. Building for Production

```bash
./mvnw clean package -DskipTests
java -jar target/mamikos-kost-api.jar --spring.profiles.active=prod
```

or build a container image without a hand-written Dockerfile:
```bash
./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=mamikos/kost-api:1.0.0
```

Required at minimum in production: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_HOST`,
`JWT_SECRET`, `SPRING_PROFILES_ACTIVE=prod`.

**Pre-flight checklist**
- [ ] `JWT_SECRET` is a freshly generated, unique 256-bit+ value (never the dev placeholder)
- [ ] TLS terminates in front of the app (HSTS is already sent, but the app itself speaks plain HTTP)
- [ ] `CORS_ALLOWED_ORIGINS` is the real frontend origin, not the default
- [ ] Database backups are scheduled
- [ ] `/actuator/health` is wired into your uptime monitoring
- [ ] Logs (JSON, via Logstash encoder) are shipped to your log aggregator

---

## 11. Architecture & Design Decisions

### Layering
```
Controller (thin) → Service (@Transactional, business rules) → Spring Data Repository → PostgreSQL
```
Enforced by an ArchUnit test (`LayeredArchitectureTest`), not just convention: the web layer
cannot depend on repositories directly, entities cannot depend on the web layer, and every
`@Entity` must store its enums as `STRING` rather than `ORDINAL` (reordering an enum must
never silently remap existing rows).

### Why several design choices are what they are

| Question | Decision | Why |
|---|---|---|
| Reset or top-up on monthly recharge? | **Reset** to full quota by default (`TOPUP` available via config) | "Recharge" for a subscription quota conventionally means restoring the full allowance, not accumulating indefinitely for a user who rarely spends |
| Owner's credit balance: zero, or no wallet at all? | **No wallet at all** | The spec says owners have no credit — modelling that as a missing row (not a `0`) makes "an owner can't spend credit" a schema-level fact, not something a forgotten `if` could violate |
| What does 5 credit actually buy? | Revealing the real `availableRooms` on that one kost, plus forwarding the question to the owner | Without this, the credit system has nothing to protect — availability would just be visible for free in the search/detail response |
| Preventing a negative balance under concurrent requests | `SELECT ... FOR UPDATE` row lock (`findByUserIdForUpdate`) around the whole deduct-and-create-inquiry sequence, backed by a DB `CHECK (balance >= 0)` | Two simultaneous inquiries reading the same "balance is enough" snapshot before either writes back is the textbook race that drives a wallet negative; the lock serialises them, the constraint is the last line of defense if it's ever bypassed |
| HTTP status for "not enough credit" | `422 UNPROCESSABLE_ENTITY` with `code: INSUFFICIENT_CREDIT` | `402 Payment Required` is semantically tempting but inconsistently handled by proxies/clients in practice; `422` stays consistent with every other domain-rule violation this API returns |
| Accessing another owner's kost | `403 NOT_KOST_OWNER`, not `404` | The `/owner/*` namespace is already authenticated, so the information leak is minor, and a specific error is far more useful while integrating than an ambiguous 404 |
| Repository pattern everywhere? | **No** — plain `JpaRepository`/`JpaSpecificationExecutor`, no hand-rolled interface wrapping them | Spring Data's repository *is* the Repository pattern already; wrapping it again adds a layer with no testing benefit and is the kind of over-engineering the brief explicitly warns against |
| Logout on a stateless JWT | Refresh token revoked **and** the specific access token's `jti` denylisted in Redis until its natural expiry | A JWT is normally valid until it expires regardless of "logout" since there's no session to delete; the denylist is the one piece of server-side state needed to make logout actually take effect immediately |

### Notable engineering pitfalls this project hit and fixed (kept here as a record, since
they're the kind of bug that looks fine until you actually load-test or restart a process)

- **Self-invocation defeats `@Transactional`.** A `REQUIRES_NEW`-annotated method called via
  `this.` from another method on the *same* bean never goes through the Spring proxy, so the
  new-transaction semantics are silently skipped. Fixed by moving the per-batch recharge
  logic into its own bean (`CreditRechargeBatchExecutor`), called through its injected proxy.
- **A rollback can undo more than the exception that caused it.** Replay-detected refresh
  tokens are deliberately revoked *and then* rejected with an exception in the same method —
  but the default rollback-on-unchecked-exception rule was undoing that revoke along with
  everything else, because the actual transaction boundary was one level up the call stack
  (`AuthService.refresh`, not `RefreshTokenService.rotate`, since the inner method just joins
  the caller's transaction under `REQUIRED` propagation). Fixed with `noRollbackFor` on the
  method that is *actually* the outermost boundary.
- **A read-only query detaches what it returns.** The recharge job's paging query loads
  wallets in one (committing) read, and a batch method mutates them in a different
  transaction — an entity that's already detached by then simply drops those mutations on
  the floor with no error at all. Fixed by re-fetching (and locking) each wallet by id inside
  the batch's own transaction, rather than passing the already-loaded entities across the
  transaction boundary.
- **A method-security exception is not the same exception a servlet filter catches.**
  `@PreAuthorize` throws `AccessDeniedException` from *inside* the controller method
  invocation, which Spring MVC resolves through `@RestControllerAdvice` before it would ever
  reach the security filter chain's own handler — so without an explicit branch for it, a
  wrong-role request came back as a generic `500` instead of `403`.

Every one of these is exercised by a permanent regression test named after the bug, not just
fixed and left undocumented (see `AuthControllerIT`, `CreditRechargeServiceIT`).

---

## 12. Project Structure

```
src/main/java/com/mamikos/kostapi/
├── auth/       registration, login, JWT, refresh-token rotation, logout denylist
├── user/       the User entity and its role
├── kost/       owner CRUD, public search (JPA Specifications), MapStruct mapping
├── credit/     wallet, ledger, recharge strategies, scheduler, manual job runner
├── inquiry/    the paid availability-inquiry flow, owner replies, domain event
└── common/     cross-cutting: security config, exception handling, rate limiting,
                request-id logging, response envelope, custom validators

src/main/resources/db/migration/       Flyway migrations (V1–V7)
src/main/resources/db/migration/dev/   demo-data seed (dev profile only, never in prod)

src/test/java/com/mamikos/kostapi/
├── architecture/   ArchUnit layering rules
├── <feature>/      unit tests (Mockito) alongside *IT integration tests (Testcontainers)
└── support/        shared test helpers (AbstractIntegrationTest, TestApi)
```

---

## 13. Troubleshooting

| Symptom | Cause / Fix |
|---|---|
| `Failed to configure a DataSource` | `DB_URL`/credentials not set, or PostgreSQL isn't running |
| `Schema-validation: missing table [kosts]` | Flyway hasn't run — check `spring.flyway.enabled=true` and that migrations are on the classpath |
| `Flyway checksum mismatch` | A migration file that already ran was edited afterward. Add a new version instead of editing an old one; in dev, `./mvnw flyway:repair` or just drop and recreate the database |
| `jwt.secret must decode to at least 256 bits` | `JWT_SECRET` is missing or too short — regenerate with `openssl rand -base64 48` |
| `WeakKeyException` at startup | Same as above |
| `Could not find a valid Docker environment` when running tests | Docker isn't running — Testcontainers needs it for `./mvnw verify` |
| `403` on an endpoint you're sure the role should access | JWT roles map to Spring Security authorities as `ROLE_<ROLE>` — check the token's `role` claim matches |
| Port `8080` already in use | `--server.port=8081`, or find and stop whatever's already bound to 8080 |
| Coverage report missing | Run `./mvnw verify` (not just `test`) — the report and its 80% gate are bound to the `verify` phase |
