# LearningHub — stub build

This is the team starter of LearningHub: the whole application with **login, the dashboard and one worked
example endpoint per service working for the admin role**. Every other endpoint — and every example when the
caller is a teacher or a student — answers **501 Not Implemented** until its owner implements it. The databases carry the full
demo dataset (the same seed as the full stack: 29 accounts, 18 subjects, enrolments, lesson plans, assessments
and invoices), so each service has real rows to implement against from day one.

## Start, stop, reset

```bash
./start.sh                 # builds the jars and images, starts the stack (first run: 5–10 minutes)
docker compose stop        # pause everything, keep the data
docker compose start       # resume
./drop-db.sh && ./start.sh # wipe all 7 databases (incl. Keycloak accounts) and re-seed from scratch
```

Requires Java 21, Maven and Docker/Podman. This stack is called `learninghub-stub`. The application shares port 8080 with the full stack in `__learninghub`
(its other ports differ: 9180 / 9761 / 6545 here versus 8180 / 8761 / 6544 there), and the podman
machine (6 GiB) cannot run both at once: stop one before starting the other.

## URLs

| What | URL |
|---|---|
| Application | http://localhost:8080 |
| Swagger UI per service | http://localhost:8080/api/`<prefix>`/swagger-ui.html — prefixes: `users`, `schedule`, `enrollment`, `learning`, `assessment`, `payments`, `dashboard` |
| OpenAPI JSON | http://localhost:8080/api/`<prefix>`/v3/api-docs |
| Keycloak admin console | http://localhost:9180/auth |
| Eureka dashboard | http://localhost:9761 |
| Postgres | localhost:6545 |

## Logins

Application users (realm `learninghub`):

| User id | Password | Role | Person |
|---|---|---|---|
| `admin1` | `admin123` | admin | Alice Admin |
| `teacher1` | `teacher123` | teacher | Mr Tom Teacher |
| `janderson` | `teacher123` | teacher | Dr John Anderson |
| `ecarter` | `teacher123` | teacher | Ms Emily Carter |
| `swilson` | `teacher123` | teacher | Ms Sarah Wilson |
| `dlee` | `teacher123` | teacher | Mr David Lee |
| `ppatel` | `teacher123` | teacher | Dr Priya Patel |
| `student1` | `student123` | student | John Smith (Junior) |
| 20 more students | `student123` | student | e.g. `ajohnson` (Freshman), `egarcia` (Sophomore), `slewis` (Junior), `jwright` (Senior) |

Every teacher on a student's schedule has a login. The dataset is described in the full stack's `seed/high-school.json`.

Keycloak admin console: user `admin`, password `password` (set `LH_KEYCLOAK_PASSWORD` to change it).

Postgres: user `pguser`, password `pgpass`; databases `userdb`, `scheduledb`, `enrollmentdb`, `learningdb`,
`assessmentdb`, `paymentdb` and `keycloak`.

```bash
psql -h localhost -p 6545 -U pguser -d enrollmentdb
# or, without a local psql:
docker exec -it learninghub-stub_postgres_1 psql -U pguser -d enrollmentdb
```

Swagger: click **Authorize**, enter one of the user ids and passwords above (client `learninghub-frontend`, no
secret). Tokens last 30 minutes.

Keycloak clients: `learninghub-frontend` (public, password flow, used by the SPA and Swagger) and
`learninghub-user-admin` (confidential, service account, used by user-service for account management; secret
`learninghub-user-admin-secret`, variable `LH_KEYCLOAK_ADMIN_CLIENT_SECRET`).

## What works and what does not

| Service | Owner | Worked example (implemented) | Everything else |
|---|---|---|---|
| dashboard-service | Sanjay Chaudhuri | `GET /api/dashboard/analytics`, `GET /api/dashboard/today` — the whole service | — |
| user-service | David Coulter | `GET /api/users/profile` — Profile page | 501 Not Implemented |
| schedule-service | Victoria Achom | `GET /api/schedule/terms` — Schedule page, term picker; also the current term for every other service | 501 Not Implemented |
| enrollment-service | Renae Nicole Weiss | `GET /api/enrollment/my-classes` — My Classes page and the Schedule page's class list, role-aware; also the course list behind the Learning and Assessments examples | 501 Not Implemented |
| learning-service | Jay Bruhn Schraml | `GET /api/learning/classes` — Learning page | 501 Not Implemented |
| assessment-service | Sai Sudha Piratla | `GET /api/assessment/summary` — Assessments page, stat tiles | 501 Not Implemented |
| payment-service | Victoria Achom | — | 501 Not Implemented |

The worked examples are the real implementation from the design, unchanged; nothing else is switched on. Where an
example reads another service that is still a stub, it uses its fallback (see the dependency table below). Class days and
times come from `GET /api/schedule/sessions`, which is still a stub, so class lists show no meeting days or times
until Victoria implements it. Teacher display names come from
`GET /api/users/teachers`, which is still a stub, so until David implements it the other services show the
teacher's username (e.g. `janderson`) instead of "Dr John Anderson" — the fallback in `enrollment/common/Remote.java`.

A stubbed endpoint answers, for example:

```json
{"status":501,"error":"Not Implemented","service":"schedule-service","owner":"Victoria Achom",
 "endpoint":"GET /api/schedule/week",
 "message":"Not Implemented: GET /api/schedule/week — schedule-service (Victoria Achom)"}
```

Security still applies before the stub: no token → 401, wrong role → 403. The contracts are real: every
controller, request body and example is in Swagger, and the database schema (`V1__schema.sql`) is in place.
The page shows an amber **Not Implemented** banner naming the endpoint and the service whenever it hits one, and a green **Implemented** strip naming the endpoint, service and owner for each implemented endpoint the page used.

## The worked example: how one request flows, and where the role is used

Try it in Swagger (`http://localhost:8080/api/learning/swagger-ui.html`, **Authorize** as `admin1`, then
`GET /api/learning/classes`): every course. Log out and authorize again as `janderson` or `student1`: the same call
answers 501 "implemented for the admin role only". The examples are switched on for admins only; the controllers
behind them are already role-aware (a teacher would get taught courses, a student enrolled courses) and that
behaviour appears as soon as the owner widens the gate. The code path is the same in every service:

1. **Login.** The SPA (`frontend/src/lib/auth.svelte.js`) posts the username and password to Keycloak
   (`/auth/realms/learninghub/protocol/openid-connect/token`, client `learninghub-frontend`) and gets a JWT whose
   `realm_access.roles` holds `admin`, `teacher` or `student`. Every API call sends it as `Authorization: Bearer`.
2. **Gateway.** gateway-service routes `/api/schedule/**` to schedule-service (found through Eureka), `/api/users/**`
   to user-service, and so on. The token is passed through untouched.
3. **Security.** `config/SecurityConfig.java` in each service validates the JWT against Keycloak and maps the
   realm roles to `ROLE_admin` / `ROLE_teacher` / `ROLE_student`. Path rules live there (for example only an admin
   may `PUT /api/schedule/sessions/**`); everything under `/api/**` needs a valid token (no token → 401, wrong role
   → 403).
4. **Stub gate.** `common/NotImplementedFilter.java` runs next: a request whose `METHOD /path` is in its
   `IMPLEMENTED` set **and whose caller has the admin role** reaches the controller and its response carries
   `X-Implemented-By: <service> (<owner>)`; anything else gets the 501 JSON with your name on it (for an
   implemented endpoint called by a teacher or student the message adds "implemented for the admin role only").
   The admin check is `isAdmin()` in that class — the place to widen when your service is ready for every role. The page shows both: a green **Implemented** strip
   listing the implemented endpoints the current page used, with service and owner, and the amber
   **Not Implemented** banner listing every 501 the current page hit (a page often calls several endpoints at once).
5. **Role-aware controller.** `common/CurrentUser.java` reads the token: `CurrentUser.isAdmin()`,
   `isTeacher()`, `isStudent()`, `username()`. The example controllers branch on it —
   `EnrollmentController.myClasses()` (behind the Learning example) lists all courses for an admin, taught courses
   for a teacher and enrolled courses for a student; `AssessmentViewController.summary()` builds different stat
   tiles for staff and for students; `ScheduleController.visible()` (behind the still-stubbed timetable views) is
   the pattern to copy: all rows for an admin, filtered by `teacherUsername` for a teacher, by enrolments for a
   student.
6. **Calling another service.** A controller never reads another service's tables. `common/ServiceClient.java`
   calls the owning service by name (`client.get("enrollment-service", "/api/enrollment/courses", …)`) and forwards
   the caller's token, so the downstream service applies the same role rules. Every call has a fallback (empty
   list, `null`, or 503 for the current term) so a page still renders when a dependency is down or still a stub.

What each worked example needs from the other services:

| Example | Calls | Via |
|---|---|---|
| `GET /api/schedule/terms` | nothing (its own table) | — |
| `GET /api/learning/classes` | `GET /api/enrollment/my-classes` (the caller's courses), `GET /api/schedule/terms` (current term) | `learning/common/Catalog.java` |
| `GET /api/assessment/summary` | `GET /api/enrollment/my-classes`, `GET /api/schedule/terms` | `assessment/common/Courses.java`, `Terms.java` |
| `GET /api/users/profile` | `GET /api/enrollment/grade-levels` (grade names — still a stub, so a student's grade shows blank); for a student also `/my-classes` and `/terms` | `user/common/Academic.java` |
| `GET /api/enrollment/my-classes` | `GET /api/schedule/terms` (current term), `/sessions` (class days and times — still a stub, falls back to none), `GET /api/users/teachers` (display names — still a stub, falls back to usernames) | `enrollment/common/Remote.java` |

Note on dates: the seed's current term is Q1 · Fall 2026 (24 Aug – 23 Oct 2026); the term picker shows it as current.

## Implementing your service

1. Make the controller method work (the controllers are the full implementation from the design; keep, change or
   rewrite them), then add its `"METHOD /path"` to `IMPLEMENTED` in
   `src/main/java/com/learninghub/<service>/common/NotImplementedFilter.java`. The path is matched exactly and the
   query string is ignored. Delete the class once every endpoint is implemented.
2. Your `src/main/resources/db/migration/V2__seed.sql` already holds the full demo data for your service; use it
   as is or change it. Schema changes go in new `V3__…sql` files — never edit an applied file (Flyway refuses to
   start on a checksum mismatch; `./drop-db.sh <db>` resets one database).
3. Update (and finally remove) the **STUB BUILD** sentence in `config/OpenApiConfig.java` so Swagger says what works.
4. Rebuild and restart just your service:

```bash
(cd services/<service> && mvn -q clean package -DskipTests)
docker compose build <service> && docker compose up -d --no-build <service>
```

Full design: slides, service contracts (with the reason behind every endpoint) and database design are in the
`design/` folder of this repository.
