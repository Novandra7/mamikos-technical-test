# MamiKos Kost API

Simple kost (boarding house) listing API, built twice with the same features so they can be compared:

- **`mamikosLaravel/`** — PHP 8.3 + Laravel 13 + MySQL + Redis, runs on port **8000**
- **`mamikosJava/`** — Java 21 + Spring Boot 3.5 + PostgreSQL + Redis, runs on port **8080**

Both do the exact same thing with the exact same endpoints. Full write-up comparing the two is in [TECHNICAL-COMPARISON.md](TECHNICAL-COMPARISON.md). Requirement docs are in `PRD-01-Laravel.md` and `PRD-02-SpringBoot.md`.

> ⚠️ **Heads up about `mamikosLaravel/`**: it's a separate git repo nested inside this one (not set up as a proper submodule yet). If you clone this repo fresh, that folder will be empty. For now just grab the two project folders directly, or ask for them as separate repos. Everything below still works either way.

## What it does

- Anyone can search kosts for free (no login needed)
- Owners can post/edit/delete their own kosts
- Regular/premium users get credit (20 or 40) and spend **5 credit** to unlock a kost's real available room count — this also sends a message to the owner
- Not enough credit = `422 INSUFFICIENT_CREDIT`, nothing gets charged
- Credit refills automatically on the 1st of every month

Every API response looks like this:

```json
{ "success": true, "message": "...", "data": {}, "meta": {} }
{ "success": false, "message": "...", "code": "INSUFFICIENT_CREDIT", "errors": {} }
```

Main endpoints (both projects, same paths, under `/api/v1`):

- `POST /auth/register`, `/auth/login`, `/auth/refresh`, `/auth/logout`, `GET /auth/me`
- `GET /kosts`, `GET /kosts/{id}` — public
- `POST/GET/PUT/DELETE /owner/kosts` — owner only
- `GET /owner/inquiries`, `POST /owner/inquiries/{id}/reply`
- `POST /kosts/{id}/availability-inquiries` — the paid one
- `GET /me/inquiries`, `/me/credits`, `/me/credits/transactions`

**Only real difference between the two:** Laravel replies in `snake_case`, Spring Boot replies in `camelCase`. Same status codes, same error codes, same everything else. See [TECHNICAL-COMPARISON.md](TECHNICAL-COMPARISON.md) for details.

## Requirements

Easiest way is Docker — you just need:

- Docker Desktop (with Compose v2)
- Git

If you'd rather not use Docker, you also need PHP 8.3 + Composer + MySQL + Redis for Laravel, or JDK 21 + PostgreSQL + Redis for Spring Boot (Maven not needed, it uses the `mvnw` wrapper).

## Running Laravel (`mamikosLaravel/`)

```bash
cd mamikosLaravel
cp .env.example .env

# install PHP deps first (needed before the docker build works)
docker run --rm -v "$(pwd)":/opt -w /opt laravelsail/php83-composer:latest composer install --ignore-platform-reqs

docker compose up -d --build
docker compose exec laravel.test php artisan key:generate
docker compose exec laravel.test php artisan migrate --seed
```

Check it's up: `curl http://localhost:8000/api/v1/health`

Useful commands:

```bash
docker compose logs -f laravel.test
docker compose exec laravel.test php artisan test
docker compose exec laravel.test php artisan route:list
```

Without Docker: `composer install`, set `DB_HOST`/`REDIS_HOST` to `127.0.0.1` in `.env`, create a `mamikos_kost` DB (plus a `testing` DB if you want to run tests), then `php artisan migrate --seed` and `php artisan serve`. You'll also want `php artisan queue:work` and `php artisan schedule:work` running in separate terminals for notifications/recharge to work.

## Running Spring Boot (`mamikosJava/`)

```bash
cd mamikosJava
cp .env.example .env
```

Then put a real JWT secret in `.env` (it won't boot without one):

```bash
openssl rand -base64 48
# paste the output as JWT_SECRET= in .env
```

```bash
docker compose up -d --build
```

Check it's up: `curl http://localhost:8080/actuator/health`

Useful commands:

```bash
docker compose logs -f app
docker compose down          # stop, keep db
docker compose down -v       # stop and wipe db
```

Without Docker: create the `mamikos_kost` Postgres DB, export `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`/`REDIS_HOST`/`JWT_SECRET`, then `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`.

## Trying it out

Both seed the same demo users (password `Password123` for all):

| Email | Role | Credit |
|---|---|---|
| owner1@mamikos.test | owner | — |
| regular@mamikos.test | regular | 20 |
| premium@mamikos.test | premium | 40 |
| lowcredit@mamikos.test | regular | 3 (to test insufficient credit) |

Quick flow to try manually:

1. Login as `regular@mamikos.test` → get the token
2. `GET /kosts` to browse, `GET /kosts/{id}` for details (available rooms hidden)
3. `POST /kosts/{id}/availability-inquiries` with the token → costs 5 credit, now shows available rooms
4. `GET /me/credits` → balance went from 20 to 15
5. Try the same with `lowcredit@mamikos.test` a few times → eventually get `422 INSUFFICIENT_CREDIT`

Laravel uses `snake_case` bodies (`price_per_month`, `password_confirmation`), Spring Boot uses `camelCase` (`pricePerMonth`, `passwordConfirmation`). There's also a Postman collection in each project's `docs/` folder, and a smoke test script at `mamikosJava/scripts/smoke-test.sh`.

## Running tests

```bash
# Laravel
cd mamikosLaravel
docker compose exec laravel.test php artisan test

# Spring Boot (needs Docker for Testcontainers)
cd mamikosJava
./mvnw test      # unit tests only
./mvnw verify    # + integration tests + coverage check
```

Both have a test that fires 10 requests at once against a 20-credit wallet to check the race condition is handled properly (should end with exactly 4 successful and balance 0).

## Code quality

```bash
# Laravel
composer format        # fix style
composer analyse       # static analysis
composer check         # both + tests

# Spring Boot
./mvnw spotless:apply     # fix style
./mvnw checkstyle:check   # lint
./mvnw verify             # everything, fails the build on violations
```

## API docs

- Laravel: `http://localhost:8000/docs/api` (Swagger-ish UI)
- Spring Boot: `http://localhost:8080/swagger-ui.html`

## Monthly credit recharge

Runs automatically on the 1st of the month. To trigger it manually instead of waiting:

```bash
# Laravel
docker compose exec laravel.test php artisan credits:recharge --dry-run
docker compose exec laravel.test php artisan credits:recharge --force

# Spring Boot (add --server.port=0 if the app is already running on 8080)
java -jar target/mamikos-kost-api.jar --spring.profiles.active=dev --job=credit-recharge --dry-run
```

## Cleaning up / resetting demo data

```bash
# Laravel
docker compose exec laravel.test php artisan migrate:fresh --seed

# Spring Boot
docker compose down -v && docker compose up -d
```

## Common problems

- **`vendor/laravel/sail/runtimes/8.3 not found`** → run the `composer install` step before `docker compose up`
- **Weird paths on Git Bash / Windows** → prefix commands with `MSYS_NO_PATHCONV=1`, or just use PowerShell
- **`Connection refused` on Laravel** → MySQL isn't ready yet, wait a bit and retry
- **`jwt.secret must decode to at least 256 bits`** → your `JWT_SECRET` is missing or too short, regenerate it
- **`port is already allocated`** → something else is using 8000/8080/3306/5432/6379/6380, change the port mapping
- **403 on Spring Boot when you expect success** → check the `role` claim in your JWT

---

Built with the help of Claude Code, based on the two PRDs in this repo.
