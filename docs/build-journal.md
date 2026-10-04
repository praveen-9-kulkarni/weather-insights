# Build journal — how this app was actually built

Not a tutorial. Each phase is: what you believed, what you asked, which files mattered, the shape at that commit. To open the **full file as it was then**:

```bash
git show <commit>:<path>
```

Example: `git show e50292e:src/main/java/com/project/weather_insights/service/WeatherService.java`

---

## 0 — Intent (before code)

**You wanted:** public API → async workers → dashboard; learn by stitching pieces; agent **guides**, you type.

**You asked:** capstone from scratch, don’t dump finished code; Spring Boot 4 / Java 21.

**Mind at the time:** “website that consumes APIs” — hadn’t split HTTP vs worker vs store yet. That split is the whole app.

---

## 1 — First HTTP (`AppHealthCheckController`)

**Commit later folded into** `e50292e`.

**You believed:** if the browser shows a page, the mapping works.

**You asked:** health returns blank in Cursor’s preview; Chrome is fine; refresh has no new logs.

**Files:** `src/main/java/com/project/weather_insights/controller/AppHealthCheckController.java`

**Shape:** `@RestController` + `@GetMapping("/health")` → `{"status":"OK"}`.

**Lesson you earned:** prove HTTP with `curl -v`. Preview ≠ API. Silent refresh can be cache.

---

## 2 — Call Open-Meteo (`WeatherService` + `WeatherController`)

**You believed:** a Java record named like “current weather” will bind the JSON.

**You asked:** TLS errors; then `Cannot map null into type double` on `temperature`; then the same on `interval`.

**Files:** `src/main/java/com/project/weather_insights/service/WeatherService.java`, `src/main/java/com/project/weather_insights/controller/WeatherController.java`

**Shape you converged on:** RestClient → vendor-shaped types (`temperature_2m`) → your API (`temperatureCelsius`, `observedAt`).

**Hiccup:** Flipkart MITM (`FK-PKI-RSA`). Fix is **trust the corp root in this JDK**, not “turn off SSL.” Same file later went into Docker `cacerts`.

---

## 3 — Postgres (`compose.yaml`, entity, repository)

**You asked:** `implementation` vs `runtimeOnly`; why Compose in the repo; “give me the entity so Hibernate creates the table.”

**Files:** `compose.yaml`, `src/main/resources/application.properties`, `src/main/java/com/project/weather_insights/model/WeatherReading.java`, `src/main/java/com/project/weather_insights/repository/WeatherReadingRepository.java`, `build.gradle`

**Mind:** DB is “a library + a URL.” Then you saw `\d weather_reading` and unique `(city, observed_at)`.

**Shape:** Compose `postgres` on `5432`; `ddl-auto` created the table (Flyway came much later).

---

## 4 — Redis queue (`WeatherJobQueue`)

**You believed:** adding the starter is enough for `StringRedisTemplate` to resolve.

**You asked:** why the type is red; Gradle reload; `POST /weather/jobs` 200 but `LRANGE` empty.

**Files:** `compose.yaml` (redis — watch the service **name**), `src/main/java/com/project/weather_insights/queue/WeatherJobQueue.java`, `src/main/java/com/project/weather_insights/controller/WeatherController.java`

**Hiccup:** Homebrew Redis + Compose Redis on 6379 — you were looking at the wrong list. After stopping Homebrew, `LPUSH`/`BRPOP` matched the app.

**Mind:** a queue is “a list in Redis,” not a framework. `BRPOP` = wait, not a spin loop.

---

## 5 — Worker process (`WeatherWorker`)

**Commit:** `e50292e` — *Initial … ingest, queue, and worker.*

**You asked:** `@Profile`; `ApplicationRunner`; unused `args`; “what is a process vs the main thread vs Hikari?”; `./gradlew bootRun --args=--spring.profiles.active=worker`.

**Files:** `src/main/java/com/project/weather_insights/worker/WeatherWorker.java`, `src/main/java/com/project/weather_insights/queue/WeatherJobScheduler.java` (`@Profile("!worker")` came in as the timer)

**Shape:**

```text
@Profile("worker")
class WeatherWorker implements ApplicationRunner {
  while (!interrupted) {
    city = queue.dequeue(timeout);  // BRPOP — thread sleeps
    weatherService.getCurrentWeather(city);
  }
}
```

**You also asked:** constructor vs `@Autowired` — constructor **is** Spring DI.

**Hiccup later:** Mac sleep paused both JVMs; unique observed_at → few rows. You expected a server; you had a laptop.

---

## 6 — Multi-city catalog

**Commit:** `a2382e5`

**You believed:** nested `Map<String, Map<String, Double>>` for lat/lon on `WeatherService`.

**You asked:** is that OK? Agent: a `City` record + one map is enough; queue payload = **city name**; unknown city is a **domain** exception, not HTTP in the service.

**Files:** `src/main/java/com/project/weather_insights/service/WeatherService.java`, `src/main/java/com/project/weather_insights/exception/UnknownCityException.java`, `src/main/java/com/project/weather_insights/queue/WeatherJobQueue.java`, `src/main/java/com/project/weather_insights/queue/WeatherJobScheduler.java`, `src/main/java/com/project/weather_insights/worker/WeatherWorker.java`, `src/main/java/com/project/weather_insights/controller/WeatherController.java`

**Mind shift:** HTTP 404 lives in the controller/advice, not in the catalog.

---

## 7 — Δ temperature

**Commit:** `37a6dc7`

**You asked:** review `WeatherReading` + persist logic.

**Files:** `src/main/java/com/project/weather_insights/model/WeatherReading.java`, `src/main/java/com/project/weather_insights/service/WeatherService.java`

**Shape:** on insert, compare to previous reading for that city; store delta. Dashboard only **reads** it.

---

## 8 — Thymeleaf dashboard

**Commit:** `ad1f298`

**Files:** `src/main/java/com/project/weather_insights/controller/WeatherDashboardController.java`, `src/main/resources/templates/dashboard.html`, `build.gradle`

**You asked:** next thing to learn — integration tests, then split fetch/persist.

---

## 9 — Test without Open-Meteo

**Commit:** `e1a96a0`

**You asked:** same package `src/test` vs `src/main`; do test inserts leak across methods?

**Files:** `src/main/java/com/project/weather_insights/service/WeatherService.java`, `src/test/java/com/project/weather_insights/service/WeatherServicePersistTest.java`

**Mind:** “private logic I can’t test” → package-private + extract. H2 is a **different** database than Compose Postgres.

---

## 10 — DLQ, then retry, then replay

**Commits:** `95e89dc` → `8b9f7dd` → `7734250`

**You asked:** what is a dead letter? How to think about nested failure? How to force a failure? Is retry a `while` in `catch`?

**Files:** `src/main/java/com/project/weather_insights/queue/WeatherJobQueue.java`, `src/main/java/com/project/weather_insights/worker/WeatherWorker.java`, `src/main/java/com/project/weather_insights/controller/WeatherController.java` (replay in `7734250`)

**Shape:** payload `city` or `city:n`; fail → increment n; `n > MAX` → another Redis list; replay `RPOP` DLQ → `LPUSH` main. Return `empty` if nothing moved.

**Mind:** reliability is “what if the handler of the failure fails,” not more `try/catch` on the happy path.

---

## 11 — City page + error split

**Commit:** `3bb12fe`

**You asked:** one handler already on REST — centralize; UI still got JSON.

**Files:** `src/main/java/com/project/weather_insights/exception/ApiExceptionHandler.java`, `src/main/java/com/project/weather_insights/exception/UiExceptionHandler.java`, `src/main/java/com/project/weather_insights/controller/WeatherDashboardController.java`, `src/main/java/com/project/weather_insights/controller/WeatherController.java`, `src/main/resources/templates/city.html`, `src/main/resources/templates/error.html`, `src/main/resources/templates/dashboard.html`

**Shape:** same `UnknownCityException`, two channels.

---

## 12 — Flyway

**Commit:** `2d4f63e`

**You asked:** aren’t SQL migrations a lock-in vs MySQL later? Shouldn’t code be DB-agnostic?

**Files:** `src/main/resources/db/migration/V1__create_weather_reading.sql`, `src/main/resources/application.properties`, `src/test/resources/application.properties`, `build.gradle`

**Mind:** Hibernate entities are not a portable schema. Tests failed until Flyway was **off** on H2.

---

## 13 — Admin token

**Commit:** `8ec43d2`, fix `61536fd`

**You asked:** `MessageDigest` vs `==`; `byte[]`; `required = false`; `${WEATHER_ADMIN_TOKEN}` loop.

**Files:** `src/main/java/com/project/weather_insights/controller/WeatherController.java`, `src/main/resources/application.properties`, `src/test/resources/application.properties`

**Shape:** `@Value("${weather.admin.token}")`, `MessageDigest.isEqual`, missing/wrong → 401. CI died until test properties defined the token.

---

## 14 — GitHub Actions

**Commit:** `3b11765`

**You asked:** sidecar services; why office CI has no `ci.yml` in GitHub.com-style; `git push -u` vs `gh pr create`.

**Files:** `.github/workflows/ci.yml`

**Shape:** on push/PR → JDK 21 → Gradle test, Postgres+Redis as **job services** (not Compose).

---

## 15 — Agent workflow

**Commits:** `232f508`, `94372bd`

**You asked:** remember “don’t commit unless I ask”; user vs project rules; teammates don’t get your **user** rules; `/commit-msg`; then `/ship` (suggest → you edit → Go).

**Files:** `.cursor/rules/agent-collaboration.mdc`, `.cursor/commands/commit-msg.md`, `.cursor/commands/ship.md`

---

## 16 — Docker app + worker

**Commit:** `8ce98b1`

**You asked:** Compose network, env vs `spring.datasource.url`, why worker still has Tomcat, registry, double-run with laptop JARs, service DNS.

**Then:** app up, worker PKIX — **same CER as the laptop**, imported in the Dockerfile.

**Files:** `Dockerfile`, `compose.yaml`, `.dockerignore`, `certs/FlipkartRootCA.cer`

**Shape:** one image; `SPRING_PROFILES_ACTIVE=worker`; env hosts `postgres`/`redis`; healthchecks before Flyway.

**Still in your head:** profile ≠ “non-web process.”

---

## How to reread in 10 minutes

1. This journal’s **Mind / You asked** lines (your questions were the curriculum).
2. `docs/lessons.md` table.
3. `git show` on the one file you would still write wrong — usually `src/main/java/com/project/weather_insights/service/WeatherService.java`, `src/main/java/com/project/weather_insights/worker/WeatherWorker.java`, or `src/main/resources/application.properties`.
