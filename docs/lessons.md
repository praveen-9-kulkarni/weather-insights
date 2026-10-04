# Weather Insights — revision sheet

Failure-mode cheat sheet from building this capstone. Full file at a given phase: `git show <commit>:<path>`. Narrative of how you got there: [build-journal.md](build-journal.md).

## Architecture

Open-Meteo → web process enqueues Redis list → worker (`@Profile("worker")`) BRPOP → Postgres → Thymeleaf dashboard + Δ-temp. Failures: retry then DLQ, admin replay (`X-Admin-Token`). Schema: Flyway V1, prod `ddl-auto=validate`. Tests: H2, Flyway off. Compose: Postgres + Redis + same image for `app` and `worker`.

HTTP process (`@Profile("!worker")` scheduler) ≠ worker process. Same JAR, profile at **runtime**. Compose DNS: `postgres`, `redis`. Laptop: `localhost` via published ports.

## What people get wrong

| Symptom / instinct | Why | What to do |
|---|---|---|
| Map API JSON onto the entity | Vendor names (`temperature_2m`) and missing keys; primitive `double`/`int` cannot be JSON `null` | API DTO → copy into domain |
| Preview pane “blank JSON” | Client, not the API | Prove with `curl -v`; refresh may be cached |
| `PKIX` / cert path | Trust store, not the URL | Import corp root into **that** JVM (`cacerts`). Docker is a new JRE — same `.cer`, `keytool` in the image. Never disable TLS verify |
| Enqueue 200, Redis empty | Two Redis on `:6379` | One listener; `lsof` / stop Homebrew |
| Container can’t reach DB | `localhost` is the container | `SPRING_DATASOURCE_URL` ≡ `spring.datasource.url` (relaxed binding); host = service name |
| New Gradle dep “missing” | IDE classpath stale | Reload Gradle Project |
| `curl ...?city=` fails in zsh | `?` is glob | Quote the URL |
| Empty table as 200 + nulls | No row is a real state | 404 (REST) or `[]`; don’t leak `ex.getMessage()` |
| One `@RestControllerAdvice` for UI too | HTML clients get JSON | Split API vs Thymeleaf error page |
| Few DB rows after hours | Unique `(city, observed_at)` + API time grain + **laptop sleep** | Servers don’t sleep; don’t debug “lost jobs” on a sleeping Mac |
| Worker “vanished” | IDE / JLS restart killed that terminal’s JVM | Other `bootRun` in another terminal lives |
| Retry in `catch` against the API | Tight loop, no backoff, still drop on Redis fail | Re-enqueue `city:n`; after N, DLQ. Replay returns `empty` vs `replayed` |
| `${WEATHER_ADMIN_TOKEN:...}` in properties | Name aliases the property → placeholder loop | Plain `weather.admin.token=`; override via env. Compare tokens with `MessageDigest.isEqual` on bytes. Missing header → 401 (`required = false`) |
| Hibernate `update` in prod | Schema drift | Flyway owns DDL; `validate` at runtime. Tests: disable Flyway on H2 |
| CI `contextLoads` dies | Token/placeholder only set locally | Test `application.properties` must resolve |
| `depends_on` then Flyway crash | Container started ≠ Postgres accepting | `pg_isready` + `service_healthy` |
| Compose Java **and** laptop Java | Two schedulers, one queue | One pair of app/worker |
| `@Profile("worker")` “isn’t a web app” | Profile does not remove `webmvc` | Tomcat still binds inside the worker container. Later: `web-application-type=none` |
| Field `@Autowired` “is Spring” | Constructor is still DI | Prefer a single constructor |

## Rules that transfer

- Catalog (city → lat/lon) in one place; queue carries **city name**.
- Service/queue have no HTTP types; `UnknownCityException` → advice.
- Persist if new `(city, observedAt)` — idempotent ingest.
- Insight in service, not the template. Split fetch vs persist so tests skip Open-Meteo.
- `src/test` same package sees package-private; H2 is not prod.
- Secrets in env. One image, two processes. CI uses the same engines you run.
- Every `catch` can fail again (DLQ when Redis is down). v1: log; later: nested try.

## Still open

Worker without Tomcat; docker profile vs env soup; Actuator readiness; Testcontainers; `docker build` in CI; don’t bake the admin token in Compose.
