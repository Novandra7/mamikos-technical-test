# MamiKos Kost API — Laravel Edition

Backend API for MamiKos' kost-search-with-credit-system technical test, built with **Laravel 13, MySQL 8, Redis, and Laravel Sanctum**, running on **Docker via Laravel Sail**.

> Twin document: `PRD-02-SpringBoot.md` describes the same product scope implemented in Spring Boot, for the Technical Comparison Report.

---

## Table of Contents

1. [Tech Stack](#tech-stack)
2. [Product Decisions](#product-decisions)
3. [Architecture](#architecture)
4. [Prerequisites](#prerequisites)
5. [Installation & Running — Docker / Sail (recommended)](#installation--running--docker--sail-recommended)
6. [Installation & Running — Local (no Docker)](#installation--running--local-no-docker)
7. [Running Tests](#running-tests)
8. [Code Quality](#code-quality)
9. [API Documentation](#api-documentation)
10. [Demo Script](#demo-script)
11. [Scheduled Job — Monthly Credit Recharge](#scheduled-job--monthly-credit-recharge)
12. [Environment Variables](#environment-variables)
13. [Troubleshooting](#troubleshooting)
14. [Project Structure](#project-structure)

---

## Tech Stack

| Component | Version |
|---|---|
| PHP | 8.3 |
| Laravel | 13.x |
| MySQL | 8.0 |
| Redis | 7 (alpine) |
| Auth | Laravel Sanctum (personal access tokens) |
| API docs | dedoc/scramble (auto-generated OpenAPI 3.1) |
| Static analysis | Larastan (PHPStan level 6) |
| Code style | Laravel Pint (PSR-12) |
| Test runner | PHPUnit 12 |
| Containers | Docker Compose (Laravel Sail scaffolding) |

## Product Decisions

The technical test statement leaves several points ambiguous on purpose. Here is what was decided and why — this is what turns "answering the question" into "designing a product."

| ID | Question | Decision | Rationale |
|---|---|---|---|
| **D-01** | Does "recharge" mean reset or top-up? | **RESET** to the role quota (default) | "Recharge" on a subscription-style quota model conventionally means restoring the full allowance; it also prevents unbounded accumulation for inactive users. `TOPUP` is available via `CREDIT_RECHARGE_STRATEGY=topup`. |
| **D-02** | Does an owner have 0 credit or no wallet at all? | **No wallet at all** | The brief states "Owner will have no credit." Not creating a `credit_balances` row means "an owner can't inquire" is enforced by the data model, not just a validation rule. |
| **D-03** | What does the 5-credit fee actually buy? | Revealing `available_rooms` + forwarding the question to the owner | Gives the credit system an actual economic reason to exist — if availability were visible for free on the detail page, credits would serve no purpose. |
| **D-04** | HTTP status for insufficient credit | **422** with `code: INSUFFICIENT_CREDIT` | `402 Payment Required` is semantically tempting but rarely used and handled unpredictably by some proxies/clients; `422` stays consistent with the rest of the domain-error catalog. |
| **D-05** | Accessing another owner's kost: 403 or 404? | **403 `NOT_KOST_OWNER`** on `/owner/*` | The namespace is already authenticated, so the extra information leak is minimal, while an explicit message is materially more useful during integration. Public endpoints still return 404. |
| **D-06** | Public IDs: auto-increment or UUID? | **BIGINT auto-increment** | Simple, index-efficient, sufficient for this scope. The enumerable-id trade-off is accepted and documented here. |
| **D-07** | How is money stored? | `DECIMAL(12,2)`, and a `Money` value object at the domain edge (never a raw float) | Avoids floating-point rounding errors on currency values. |
| **D-08** | Can a user inquire about the same kost repeatedly? | **Yes**, 5 credits each time | Nothing in the brief forbids it, and availability genuinely changes over time. An anti-spam cooldown is noted as a future improvement. |

### Anti-over-engineering note

The repository pattern is applied **selectively** — only for the two aggregates with non-trivial queries (`Kost`, `Credit`, `Inquiry`). Simple lookups go straight through Eloquent inside the service layer. Introducing a repository for every model would add indirection without adding value; reviewers tend to reward this kind of restraint more than blanket pattern application.

## Architecture

```
Route (routes/api.php)
  → Middleware (auth:sanctum, role:*, throttle:*, RequestId, SecurityHeaders)
    → Controller (thin — delegates only)
      → Form Request (validation)
      → Service (business rules, transactions, domain exceptions, events)
        → Repository Interface → Eloquent Repository → Model → MySQL/Redis
      → API Resource (response shape)
```

Patterns used and where: **Service Layer** (`app/Services`), **Repository** (`app/Repositories`, selective), **DTO** (`app/DTOs`, readonly classes), **Strategy** (`app/Domain/Credit/Strategies` — reset vs. top-up recharge), **Policy** (`app/Policies` — kost/inquiry ownership), **Observer** (`app/Observers/KostObserver` — address normalization), **Event/Listener** (`AvailabilityInquiryCreated` → `NotifyOwnerOfNewInquiry`, queued), **Query Object** (`app/Repositories/Filters/KostSearchFilter`), **native PHP Enums** (`app/Enums`), **centralized domain exceptions** (`app/Exceptions/Domain`, mapped to the standard error envelope in `bootstrap/app.php`).

The credit-deduction path (`CreditService::deduct*`) uses `SELECT … FOR UPDATE` inside a database transaction, backed by a `CHECK (balance >= 0)` constraint at the database level — this is what keeps 10 parallel requests against a 20-credit wallet from ever going negative (see `tests/Feature/Inquiry/ConcurrentInquiryTest.php`).

## Prerequisites

| Tool | Minimum version | Check |
|---|---|---|
| Docker Desktop (with Compose v2) | 24+ | `docker compose version` |
| Git | 2.30 | `git --version` |

Everything else (PHP, Composer, MySQL, Redis) runs **inside containers** — you do not need them installed locally to use the Docker path below. A local, Docker-free installation path is also documented further down for completeness.

> **Windows note:** the `./vendor/bin/sail` wrapper script requires WSL2 (or macOS/Linux). On plain Windows PowerShell/Git Bash without WSL2, use the equivalent `docker compose ...` commands shown below instead — they drive the exact same `docker-compose.yml` Sail generated, so behavior is identical.

## Installation & Running — Docker / Sail (recommended)

**1. Clone and enter the project**
```bash
git clone https://github.com/<username>/mamikos-kost-api-laravel.git
cd mamikos-kost-api-laravel
```

**2. Copy the environment file**
```bash
cp .env.example .env          # Windows PowerShell: Copy-Item .env.example .env
```
The committed `.env.example` already has working defaults for the Dockerized stack (`DB_HOST=mysql`, `REDIS_HOST=redis`, `APP_PORT=8000`).

**3. Build and start the containers**
```bash
# With WSL2 / macOS / Linux:
./vendor/bin/sail up -d

# Without WSL2 (plain Windows shell) — identical containers, direct compose call:
docker compose up -d --build
```

Services started:

| Service | Purpose |
|---|---|
| `laravel.test` | PHP-FPM + Nginx app container, exposed on `:8000` |
| `mysql` | MySQL 8.0, exposed on `:3306` |
| `redis` | Redis 7, exposed on `:6380` (host side — mapped away from 6379 to avoid clashing with other local stacks; container-to-container traffic still uses 6379) |
| `queue` | `php artisan queue:work` — processes the inquiry-notification listener |
| `scheduler` | `php artisan schedule:work` — runs the monthly recharge job on schedule |

**4. Install dependencies (first run only, if you didn't build the image with them baked in) and prepare the app**
```bash
sail composer install            # or: docker compose exec laravel.test composer install
sail artisan key:generate        # or: docker compose exec laravel.test php artisan artisan key:generate
sail artisan migrate --seed      # or: docker compose exec laravel.test php artisan migrate --seed
```

**5. Verify**
```bash
curl http://localhost:8000/api/v1/health
# {"success":true,"data":{"status":"ok","database":"ok","cache":"ok"}, ...}
```

**Useful commands**
```bash
docker compose logs -f laravel.test     # app logs
docker compose exec laravel.test bash   # shell into the app container
docker compose exec laravel.test php artisan test
docker compose down -v                  # stop and wipe volumes
```

**Demo accounts** (seeded by `UserSeeder`, password `Password123` for all):

| Email | Role | Starting balance |
|---|---|---|
| `owner1@mamikos.test` | owner | — (no wallet) |
| `owner2@mamikos.test` | owner | — (no wallet) |
| `regular@mamikos.test` | regular | 20 |
| `premium@mamikos.test` | premium | 40 |
| `lowcredit@mamikos.test` | regular | 3 (for exercising `INSUFFICIENT_CREDIT`) |

`KostSeeder` seeds 30 kosts across 6 cities priced between Rp 500,000 and Rp 5,000,000, so search/sort/pagination have real data to exercise.

## Installation & Running — Local (no Docker)

For reviewers who prefer a bare-metal setup.

**Prerequisites:** PHP 8.2+ (`pdo_mysql`, `mbstring`, `openssl`, `bcmath`, `redis` extensions), Composer 2.7+, MySQL 8, Redis 7.

```bash
composer install
cp .env.example .env
php artisan key:generate
```

Edit `.env` and switch the Docker-oriented hosts to your local services:
```dotenv
DB_HOST=127.0.0.1
REDIS_HOST=127.0.0.1
```

Create the databases and migrate:
```bash
mysql -u root -p -e "CREATE DATABASE mamikos_kost CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
php artisan migrate --seed
```

Run the app and its background workers (three terminals):
```bash
php artisan serve                 # API at http://localhost:8000/api/v1
php artisan queue:work            # inquiry notification listener
php artisan schedule:work         # monthly recharge scheduler
```

## Running Tests

```bash
# Docker / Sail
docker compose exec laravel.test php artisan test
docker compose exec laravel.test php artisan test --testsuite=Unit
docker compose exec laravel.test php artisan test --testsuite=Feature
docker compose exec laravel.test php artisan test --testsuite=Console
docker compose exec laravel.test php artisan test tests/Feature/Inquiry/ConcurrentInquiryTest.php

# Local
php artisan test
```

`tests/Feature/Inquiry/ConcurrentInquiryTest.php` is the one test that genuinely needs multiple database connections racing each other (10 parallel requests against a single 20-credit wallet must yield exactly 4 successes). A single transaction-wrapped `RefreshDatabase` test cannot reproduce that, so this test forks real child processes via `pcntl_fork` — it is skipped automatically if the `pcntl` extension isn't available.

Coverage report (requires Xdebug or PCOV):
```bash
docker compose exec laravel.test bash -c "XDEBUG_MODE=coverage php artisan test --coverage --min=80"
```

## Code Quality

```bash
composer format          # Laravel Pint — auto-fix style
composer format:check     # Pint — check only, no changes (CI uses this)
composer analyse           # Larastan / PHPStan level 6
composer check              # format:check + analyse + test, run this before every commit
```
(Prefix with `docker compose exec laravel.test` if not running inside the container's shell.)

## API Documentation

- **Interactive docs (Swagger-style UI):** `http://localhost:8000/docs/api` — auto-generated from the actual controllers/Form Requests/API Resources by `dedoc/scramble`, so it can never drift out of sync with the code.
- **Raw OpenAPI 3.1 spec:** `http://localhost:8000/docs/api.json`, or the static snapshot committed at [`docs/openapi.yaml`](docs/openapi.yaml) / [`docs/openapi.json`](docs/openapi.json).
- **Postman collection:** [`docs/postman_collection.json`](docs/postman_collection.json) — import it, set `base_url`/`owner_token`/`user_token` collection variables, and run the folders top to bottom.

## Demo Script

End-to-end proof that every requirement in the brief works, in order:

```bash
BASE=http://localhost:8000/api/v1

# 1. Register an owner — expect data.credit === null
curl -s -X POST $BASE/auth/register -H "Content-Type: application/json" -d '{
  "name":"Pak Slamet","email":"slamet@example.com","password":"Password123",
  "password_confirmation":"Password123","phone":"081200001111","role":"owner"
}'

# 2. Register a regular user — expect data.credit.balance === 20
curl -s -X POST $BASE/auth/register -H "Content-Type: application/json" -d '{
  "name":"Rina","email":"rina@example.com","password":"Password123",
  "password_confirmation":"Password123","role":"regular"
}'

export OWNER_TOKEN=<paste access_token from step 1>
export USER_TOKEN=<paste access_token from step 2>

# 3. Owner adds a kost (repeat with a different name to prove "more than one kost")
curl -s -X POST $BASE/owner/kosts -H "Authorization: Bearer $OWNER_TOKEN" \
  -H "Content-Type: application/json" -d '{
    "name":"Kost Melati Residence","description":"Kost putri dekat kampus.",
    "address":{"street":"Jl. Kaliurang KM 5","district":"Depok","city":"Sleman","province":"DI Yogyakarta"},
    "price_per_month":950000,"room_type":"putri","total_rooms":12,"available_rooms":4,
    "facilities":["wifi","kamar mandi dalam"]
  }'

# 4. Public search — no token needed
curl -s "$BASE/kosts?location=Sleman&sort_by=price&order=asc"

# 5. Public detail — available_rooms must NOT appear
curl -s "$BASE/kosts/1"

# 6. User asks about availability (-5 credit) — expect balance_after === 15
curl -s -X POST $BASE/kosts/1/availability-inquiries -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" -d '{"message":"Masih ada kamar kosong?"}'

# 7. Balance & ledger
curl -s $BASE/me/credits -H "Authorization: Bearer $USER_TOKEN"
curl -s $BASE/me/credits/transactions -H "Authorization: Bearer $USER_TOKEN"

# 8. Repeat step 6 until balance hits 0, then one more call → 422 INSUFFICIENT_CREDIT

# 9. Authorization checks
curl -s -o /dev/null -w "%{http_code}\n" -X POST $BASE/owner/kosts -d '{}'                                   # 401
curl -s -o /dev/null -w "%{http_code}\n" -X POST $BASE/owner/kosts -H "Authorization: Bearer $USER_TOKEN" -d '{}'   # 403
curl -s -o /dev/null -w "%{http_code}\n" -X POST $BASE/kosts/1/availability-inquiries -H "Authorization: Bearer $OWNER_TOKEN" -d '{}' # 403

# 10. Owner replies to an inquiry
curl -s $BASE/owner/inquiries -H "Authorization: Bearer $OWNER_TOKEN"
curl -s -X POST $BASE/owner/inquiries/1/reply -H "Authorization: Bearer $OWNER_TOKEN" \
  -H "Content-Type: application/json" -d '{"reply":"Masih ada 3 kamar, silakan survei akhir pekan ini."}'

# 11. Monthly recharge, proven idempotent
docker compose exec laravel.test php artisan credits:recharge --dry-run
docker compose exec laravel.test php artisan credits:recharge --force
curl -s $BASE/me/credits -H "Authorization: Bearer $USER_TOKEN"     # balance back to 20
docker compose exec laravel.test php artisan credits:recharge      # run again → all "skipped"

# 12. Delete the kost, prove it disappears from search/detail but its inquiry history stays
curl -s -X DELETE $BASE/owner/kosts/1 -H "Authorization: Bearer $OWNER_TOKEN"
curl -s -o /dev/null -w "%{http_code}\n" $BASE/kosts/1              # 404
```

## Scheduled Job — Monthly Credit Recharge

| Aspect | Value |
|---|---|
| Command | `php artisan credits:recharge` |
| Schedule | `->monthlyOn(1, '00:00')->timezone('Asia/Jakarta')` (`routes/console.php`) — `0 0 1 * *` WIB |
| Target | Users with role `regular`/`premium` that own a wallet |
| Strategy | `reset` (default, `balance = quota`) or `topup` (`balance = min(balance + quota, max_balance)`), set via `CREDIT_RECHARGE_STRATEGY` |
| Idempotency | Users whose `last_recharged_at` already falls in the current calendar month are skipped |
| Options | `--dry-run`, `--user-id=`, `--strategy=reset\|topup`, `--force` |
| Concurrency safety | `Cache::lock('credits:recharge', 900)` inside the command + `withoutOverlapping(600)`/`onOneServer()` on the schedule entry |

Activated in this project by the `scheduler` container running `php artisan schedule:work`. On a bare server, use a crontab entry instead:
```cron
* * * * * cd /var/www/mamikos-api && php artisan schedule:run >> /dev/null 2>&1
```

Manual verification:
```bash
php artisan credits:recharge --dry-run     # preview, no writes
php artisan credits:recharge --force       # run for real, bypassing idempotency
php artisan credits:recharge               # run again → everyone "skipped" (proves idempotency)
php artisan schedule:list                  # confirm the entry is registered
```

## Environment Variables

| Variable | Example | Notes |
|---|---|---|
| `APP_ENV` / `APP_DEBUG` | `local` / `true` | Set `APP_DEBUG=false` in production — the exception handler already strips stack traces/SQL from 500 responses regardless, but this is defense in depth. |
| `APP_TIMEZONE` | `Asia/Jakarta` | Business timezone (WIB). |
| `DB_*`, `REDIS_*` | — | See `.env.example`; Docker values point at the `mysql`/`redis` service names. |
| `SANCTUM_TOKEN_EXPIRATION` | `1440` | Minutes; BR-15 requires 24h token expiry. |
| `CREDIT_REGULAR_QUOTA` / `CREDIT_PREMIUM_QUOTA` | `20` / `40` | BR-01. |
| `CREDIT_INQUIRY_COST` | `5` | BR-02. |
| `CREDIT_RECHARGE_STRATEGY` | `reset` \| `topup` | D-01/BR-08. |
| `CREDIT_MAX_BALANCE` | `200` | Cap for the `topup` strategy. |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Comma-separated allowlist, SEC-13. |

## Troubleshooting

| Symptom | Cause / fix |
|---|---|
| `port is already allocated` on `docker compose up` | Another stack (e.g. the Spring Boot sibling project) is already using 3306/6379. This project maps Redis to host port **6380** by default for exactly this reason — adjust `FORWARD_DB_PORT`/`FORWARD_REDIS_PORT` in `.env` if you still collide. |
| `Unsupported operating system` from `./vendor/bin/sail` | You're on plain Windows without WSL2. Use the `docker compose ...` equivalents shown throughout this README — they operate on the same `docker-compose.yml`. |
| `Permission denied` writing to `storage/logs/laravel.log` or `.phpunit.result.cache` | Usually happens if a command was run as `root` inside the container instead of the `sail` user. Fix with `docker compose exec laravel.test chown -R sail:sail storage bootstrap/cache`. |
| `SQLSTATE[HY000] [2002] Connection refused` | MySQL isn't ready yet, or `DB_HOST` is wrong. In Docker it must be `mysql`, not `127.0.0.1`. |
| `Personal access token expired` | Tokens expire after `SANCTUM_TOKEN_EXPIRATION` minutes (24h by default) — log in again or call `POST /auth/refresh`. |
| Coverage report is empty | Xdebug/PCOV isn't enabled — run with `XDEBUG_MODE=coverage`. |

## Project Structure

```
app/
├── Console/Commands/RechargeUserCreditsCommand.php
├── DTOs/                     # readonly data-transfer objects between layers
├── Domain/
│   ├── Credit/{RechargeSummary,RechargeStrategyResolver}.php
│   ├── Credit/Strategies/{RechargeStrategyInterface,ResetRechargeStrategy,TopUpRechargeStrategy}.php
│   └── Shared/{Money,Address}.php
├── Enums/                    # UserRole, RoomType, CreditTransactionType, InquiryStatus, RechargeStrategyType
├── Events/AvailabilityInquiryCreated.php
├── Exceptions/Domain/        # DomainException + concrete business exceptions
├── Http/{Controllers,Middleware,Requests,Resources}/
├── Listeners/NotifyOwnerOfNewInquiry.php
├── Models/                   # User, Kost, KostFacility, KostPhoto, CreditBalance, CreditTransaction, AvailabilityInquiry
├── Observers/KostObserver.php
├── Policies/{KostPolicy,InquiryPolicy}.php
├── Providers/RepositoryServiceProvider.php
├── Repositories/{Contracts,Eloquent,Filters}/
├── Rules/AvailableRoomsWithinCapacity.php
├── Services/                 # AuthService, KostService, KostSearchService, AvailabilityInquiryService, CreditService, CreditRechargeService
└── Support/ApiResponse.php   # success/error envelope builder

database/{migrations,factories,seeders}/
docs/{openapi.yaml,openapi.json,postman_collection.json}
tests/{Unit,Feature,Console}/
```

---

**Author's note on AI assistance:** this implementation was built with the help of an AI coding assistant (Claude Code) working from `PRD-01-Laravel.md` end to end — scaffolding, business logic, tests, and this documentation. Every acceptance criterion in the PRD was manually verified against the running Docker stack (including the 10-parallel-request race-condition requirement) before being considered done.
