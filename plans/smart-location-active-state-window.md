# SmartLocation `ACTIVE` state driven by an activation window

## Context

Today a SmartLocation is served to other parties only when it is `VERIFIED`, and `VERIFIED` is set
purely by hand — an explicit `PATCH { "smart_location_state": "VERIFIED" }` from the
transit-dashboard. There is no notion of *when* a location is supposed to be live. The
`SmartLocationState` enum already carries an unused `ACTIVE` constant.

This change makes activation time-bound. A smart location gets an **activation window**
(`activeFirstDay` / `activeLastDay`). A daily job in both `banula-nsp` and `banula-cdr-adapter` runs
at **00:00:05 Europe/Berlin** and flips locations between `VERIFIED` and `ACTIVE` depending on
whether *today* falls inside that window (both edge days inclusive). `ACTIVE` is never assigned by a
human — the dashboard lets an operator define the window, and the system decides the state. The NSP
endpoints that serve locations to other parties then serve `ACTIVE` instead of `VERIFIED`.

### Decisions confirmed by the user

| Question | Decision |
|---|---|
| Promotion / demotion | **`VERIFIED` ⇄ `ACTIVE` only.** In-window + `VERIFIED` → `ACTIVE`; out-of-window + `ACTIVE` → `VERIFIED`. `PLAIN_OCPI`/`ENRICHED`/`INVALID`/`ARCHIVED` are never auto-activated — manual states always win. |
| Dashboard save | PATCH sends **only the two dates**, never the state. NSP re-evaluates immediately on save, so a window covering today flips to `ACTIVE` in the same request. |
| cdr-adapter job | **Re-pull from NSP first, then evaluate** the local mirror with identical logic. |
| Removing a window | Selecting any other state in the dropdown **clears both dates** server-side. |

### Repos touched

`banula-open-library` → `private-lib` → `banula-nsp` + `banula-cdr-adapter` → `transit-dashboard`.

---

## 1. banula-open-library (1.2.0 → 1.2.1)

### 1.1 New `LocalDate` serializers

openlib has **zero `LocalDate`** today (110 × `LocalDateTime`). The house convention is never
`@JsonFormat` — always an explicit `@JsonSerialize`/`@JsonDeserialize` pair — precisely because
openlib is a library consumed by 7+ services with independently configured `ObjectMapper`s. Add to
`ocpi/util/`, mirroring the existing `OCPILocalDateTime*` pair:

- `OCPILocalDateSerializer` — writes `yyyy-MM-dd`.
- `OCPILocalDateDeserializer` — parses `ISO_LOCAL_DATE`, falling back to taking the date part of a
  full ISO date-time, else throws `IOException`.

> Correction to an earlier assumption: `GenericMongoMapper`'s bare `ObjectMapper` (line 41) is used
> **only** at line 276 to parse the `@CompoundIndex.def` string — entity↔DTO conversion goes through
> `BeanUtils.copyProperties` (reflection, no Jackson). So a raw `LocalDate` would not crash there.
> The serializers are still the right call for convention and for the HTTP boundary, but this is not
> a crash-avoidance measure.

### 1.2 Two new fields

Append to `ocpi/custom/smartlocations/SmartLocation.java` and, identically, to
`dto/SmartLocationDTO.java` — **no validation group**, these are optional:

```java
@JsonProperty("active_first_day")
@JsonSerialize(using = OCPILocalDateSerializer.class)
@JsonDeserialize(using = OCPILocalDateDeserializer.class)
private LocalDate activeFirstDay;

@JsonProperty("active_last_day")
@JsonSerialize(using = OCPILocalDateSerializer.class)
@JsonDeserialize(using = OCPILocalDateDeserializer.class)
private LocalDate activeLastDay;
```

`MongoSmartLocation` (adds only `@Id`) and `SmartLocationMapper` (`BeanUtils.copyProperties`) need
**no change**. Note NSP sets `spring.jackson.default-property-inclusion: NON_NULL`, so an unset
window is *absent* from the JSON, not `null` — the frontend must treat `undefined` as "no window".

### 1.3 Mongo converter — required, not optional

`mongodb/config/MongoConfig.java` registers UTC-pinned `LocalDateTime ↔ Date` converters but none for
`LocalDate`. Without one, Spring Data falls back to `Jsr310Converters`, which pins to
`ZoneId.systemDefault()`: a JVM on `Europe/Berlin` writes `2026-08-24` as `2026-08-23T22:00:00Z`, and
a UTC reader reads back `2026-08-23` — **an off-by-one day at exactly the boundary this feature is
about.** Add a UTC-pinned `LocalDate ↔ Date` pair to `customConversions()`, mirroring the existing
`LocalDateTime` pair. Storage is a BSON `Date` at `00:00:00.000Z`, which also keeps future
server-side range queries possible.

### 1.4 Shared evaluation logic — one implementation, both services

Both services must produce bit-identical answers or the mirror desynchronises, and the workspace
rules forbid duplication. New
`ocpi/custom/smartlocations/util/SmartLocationActivationUtil.java` — static, no Spring, same shape as
`ModelPatcherUtil`:

```java
public static LocalDate today(String zoneId);                        // falls back to Europe/Berlin
public static boolean hasActivationWindow(SmartLocation location);   // BOTH days set
public static boolean isWindowValid(SmartLocation location);         // first <= last
public static boolean isWithinActiveWindow(SmartLocation l, LocalDate today);  // inclusive both edges
public static SmartLocationState resolveState(SmartLocation l, LocalDate today);
public static boolean applyActiveState(SmartLocation l, LocalDate today);  // true iff state changed
public static boolean isPubliclyServable(SmartLocationState state);       // state == ACTIVE
```

**`resolveState` transition table:**

| current state | in window | → | rationale |
|---|---|---|---|
| `VERIFIED` | yes | `ACTIVE` | the only promotion path |
| `VERIFIED` | no | `VERIFIED` | unchanged |
| `ACTIVE` | yes | `ACTIVE` | unchanged |
| `ACTIVE` | no | `VERIFIED` | window ended → fall back; verification is still true |
| `null`, `PLAIN_OCPI`, `ENRICHED`, `INVALID`, `ARCHIVED` | either | unchanged | manual states always win |

Per the user's wording, **both** days must be set to count as a window; one day alone does nothing.
Windows where `first > last` never activate and log a `WARN` so bad data is visible.

This makes the job **idempotent** (a second run changes nothing) and **reversible**, which is what
makes the on-demand verification in §6 possible without touching the clock.

`isPubliclyServable` is the single place that answers "may this be served to other parties?" — used
by the `publish` flag and by both sender endpoints, so a future policy change is one edit.

### 1.5 Tests & version

New `SmartLocationActivationUtilTest` (both edges inclusive, one-day window, day before/after, only
one day set, inverted window, all six source states, idempotency); extend the existing
`ModelPatcherUtilTest` for the two new fields. `pom.xml` line 6 → `1.2.1`.

---

## 2. Version bump + install order

`install-local-javalibs.sh` aborts private-lib on an openlib pin mismatch and skips any service whose
pin doesn't match, so make **all four pom edits first**, then run it once.

| # | File | Line | From → To |
|---|---|---|---|
| 1 | `banula-open-library/pom.xml` | 6 | `1.2.0` → `1.2.1` |
| 2 | `private-lib/pom.xml` | 152 (openlib pin) | `1.2.0` → `1.2.1` |
| 3 | `private-lib/pom.xml` | 6 | `1.4.1` → `1.4.2` |
| 4 | `banula-nsp/pom.xml` | 69 | openlib `1.2.0` → `1.2.1` |
| 5 | `banula-cdr-adapter/pom.xml` | 29 | `private-library` `1.4.1` → `1.4.2` |

Then `transit-local-services/scripts/install-local-javalibs.sh nsp cdr-adapter` — passing the two
target services rather than `all`, so the five services still on the old pins don't fill the summary
with skip warnings. **Re-run it after every openlib edit**: cdr-adapter resolves openlib
*transitively through* private-lib, so private-lib must be reinstalled even though its own code
never changes.

---

## 3. banula-nsp

### 3.1 Repository

`repository/SmartLocationRepository.java`:
- Line 34 `@Query`: `'smartLocationState': 'VERIFIED'` → `'ACTIVE'`; rename
  `findVerifiedSmartLocations` → `findActiveSmartLocations` (one caller) and fix its Javadoc.
- Add the derived query `List<MongoSmartLocation> findBySmartLocationStateIn(Collection<SmartLocationState> states);`

### 3.2 Re-evaluation service method

Add to `NSPSmartLocationService` / `NSPSmartLocationServiceImpl` (the scheduler must not touch a
repository — layering rule):

```java
/** Re-evaluates VERIFIED/ACTIVE locations against today in the configured zone. Idempotent. */
int refreshActiveStates();
```

Loads candidates via `findBySmartLocationStateIn(VERIFIED, ACTIVE)`, calls `applyActiveState`, and
**saves only when the state actually changed**, also updating `publish` and `lastUpdated`.

> Saving only on change matters: `findActiveSmartLocations` filters on `lastUpdated`, which is the
> cursor OCPI clients page on. Touching every row nightly would make every location look "updated"
> to every roaming partner every day.

`MongoSmartLocation extends SmartLocation`, so candidates can be evaluated and saved without a
`toMongo` round-trip. "Today" comes from `ApplicationConfiguration.getZoneId()` (already
`Europe/Berlin`) — never hardcoded.

### 3.3 Update endpoint

In `NSPSmartLocationServiceImpl.patchSmartLocation` (lines 64-120):

1. **Reject a manually-sent `ACTIVE`** with `OCPICustomException(..., STATUS_CODE_INVALID_OR_MISSING_PARAMETERS)`
   — enforces "ACTIVE is never set directly" at the API. The dashboard never sends it.
2. Assign the requested state (existing lines 86-88).
3. `ModelPatcherUtil.smartLocationPatcher(...)` — carries the two dates in (existing).
4. **New, after the patcher:** if the request explicitly asked for a state, clear both dates. This is
   the confirmed "clear on leaving ACTIVE" behaviour, and it sidesteps the fact that the patcher
   copies non-null only and can never clear a field.
5. Existing `PLAIN_OCPI → ENRICHED` auto-promotion (lines 103-107), untouched.
6. **New:** `applyActiveState(...)` so a window covering today takes effect immediately on save.
7. `publish = isPubliclyServable(state)` — replacing the inline `== VERIFIED` at lines 90-95, and
   moved to *after* steps 5-6 since both can change the state.

Extract the "evaluate → publish → stamp → save → map to DTO" tail into one private helper shared by
`patchSmartLocation` and `refreshActiveStates`. Requires injecting `ApplicationConfiguration` into
this service (already `@AllArgsConstructor`).

A window saved while the location is still `ENRICHED` is stored but inert until it is verified —
surfaced in the UI copy.

### 3.4 Serving other parties

`service/NSPLocationServiceImpl.java`:

- **List interface** — line 296 in `findLocations()` → `findActiveSmartLocations(...)`. Serves
  `GET /api/v1/internal/ocpi/2.2.1/locations`.
- **Single-object interface** — `getLocationEvseConnector()` (lines 41-88) applies **no** state
  filter today, so any party can read a `PLAIN_OCPI` or `INVALID` location. Gate it on
  `isPubliclyServable`, throwing the *same* `OCPICustomException("Location not found")` as a genuinely
  missing location (leaking "exists but not active" is information disclosure to a counterparty).

  ⚠️ **It has three internal callers** — `putEvse` (line 95), `patchEvse` (line 157) and
  `putConnector` (line 163) all use it for read-modify-write. Gating it unconditionally would break
  all three OCPI receiver write paths. Extract the current body into a private un-gated
  `getLocationEvseConnectorInternal(...)`, have those three call it, and let the public method do
  the guard then delegate. No duplication.

- `putLocation():255-256` (`PLAIN_OCPI` + `publish=false`) stays as-is — still correct.

### 3.5 Scheduler

Follow the existing convention exactly (`tasks/` Runnable + `config/SchedulerConfig.java`):

- New `tasks/SmartLocationActiveStateCheck.java implements Runnable` (mirroring
  `RemoteStillAliveCheck`), calling `refreshActiveStates()` and logging the count.
- In `SchedulerConfig`, a second `@Scheduled` method with the same flag-guard + try/catch shape:

```java
@Scheduled(cron = "${active-state-check.cron:5 0 0 * * *}", zone = "${api.zone-id}")
```

`5 0 0 * * *` = 00:00:05; Spring's `@Scheduled` supports `zone` natively, so NSP gets Berlin
correctness for free by reusing the existing `api.zone-id` property.

- `ApplicationConfiguration`: `@Value("${active-state-check.enabled:true}")`.
- `application.yml`, after the `remote-check` block:

```yaml
active-state-check:
  enabled: ${ACTIVE_STATE_CHECK_ENABLED:true}
  cron: ${ACTIVE_STATE_CHECK_CRON:5 0 0 * * *}
```

Both have defaults, so startup can't fail. NSP has **no** `verify-env-vars.yaml` workflow (only
build-push, checks, override-dev, pr-source-check, tag), so there is no CI gate either way; still add
both keys to `infra/helm/values.yaml` for parity with the `REMOTE_CHECK_*` precedent. NSP's k8s
ConfigMap lives in the separate `common-infra-resources` repo — flag whether it needs them too.

### 3.6 Manual trigger

`POST /api/v1/internal/locations/refresh-active-states` on `NonOcpiSmartLocationController`,
delegating to `refreshActiveStates()`. It invokes the *identical* method the scheduler calls, so it
is what makes §6 provable without waiting for midnight, and it mirrors cdr-adapter's existing
`POST /smart-locations/update`.

### 3.7 Tests

NSP has only a context-load test, so the logic coverage lives in openlib (§1.5). Add focused
JUnit 5 + Mockito tests for `refreshActiveStates` and the patch reordering.

---

## 4. banula-cdr-adapter

### 4.1 🔴 Refresh the generated NSP client — mandatory, and there are *two* failures

`client/NavigationServiceClient.queryAllSmartLocations()` deserializes into an **openapi-generated**
`SmartLocationDTO` compiled from the checked-in
`src/main/resources/openapi/navigation-service-api.json`, then converts to the openlib DTO. Verified
state of that spec today:

```json
{"enum":["PLAIN_OCPI","ENRICHED","VERIFIED","INVALID","ARCHIVED"],"type":"string"}
```

1. The two new fields are absent → dropped at the HTTP boundary → cdr-adapter's mirror never sees a
   window → its job finds nothing to do, silently, forever.
2. **Worse: `ACTIVE` is already missing from the enum.** openapi-generator's `fromValue()` throws on
   an unknown value, and `queryAllSmartLocations()` swallows every exception and returns `null`. The
   moment NSP serves `"ACTIVE"`, `pullSmartLocations()` logs `"No smart locations received"` and
   returns — the manual `POST /update` becomes a silent no-op. (`deleteAll()` sits *after* the null
   check, so there is no data loss.)

**⇒ NSP and cdr-adapter must ship together.** Procedure: finish openlib+NSP → install libs →
`banula-nsp/./start.sh` → `banula-cdr-adapter/scripts/update-navigation-api.sh` → review the `jq -S`
diff and confirm `ACTIVE` plus `{"format":"date","type":"string"}` for both new fields →
`./mvnw clean compile`. cdr-adapter's `ObjectMapper` already has `JavaTimeModule` with
`WRITE_DATES_AS_TIMESTAMPS` disabled, so `LocalDate` round-trips as `"2026-08-24"`.

### 4.2 Evaluation method

`SmartLocationService.refreshActiveStates()` — identical shape to §3.2, over
`findBySmartLocationStateIn(VERIFIED, ACTIVE)` (add the derived query to cdr's
`SmartLocationRepository`). This is cdr's first use of `SmartLocationState`. Plus
`POST /api/v1/internal/smart-locations/refresh-active-states` on `NonOcpiSmartLocationsController`,
following the shape of the existing `updateSmartLocations()`.

### 4.3 Scheduler

cdr-adapter has **no `@Scheduled` and no `@EnableScheduling`** — schedules live only in
`schedulers/DynamicScheduler.java`. Three surgical edits:

- **Extract `reschedule(future, job, cron, zone)`.** `scheduleProcessMsconsFiles` (68-73) and
  `scheduleProcessDailyResults` (75-81) are already byte-identical; a third copy would violate the
  no-duplication rule. This is also where the timezone gets threaded in.
- **Seconds:** `buildDailyCronFromTime` hardcodes second `0`. Add an overload
  `buildDailyCronFromTime(String hhmm, int second)` and let the existing one delegate with `0`;
  existing callers untouched.
- **Timezone:** existing `CronTrigger`s are built without a zone and fire in the JVM default (UTC).
  Pass `TimeZone.getTimeZone("Europe/Berlin")` for the **new job only**.

  > **Do not silently move the existing jobs to Berlin.** `msconsProcessingTriggerTime` defaults to
  > `00:00` and fires at 00:00 **UTC** today; flipping the scheduler wholesale would shift settlement
  > timing by 1-2h — an unrelated, unreviewed behaviour change. Separate ticket (§7.5).

Job body in house style — flag guard → early return with log → whole body in `try/catch` that only
logs. Per the user's decision it calls `pullSmartLocations()` **then** `refreshActiveStates()`, with
the pull in its **own** inner `try/catch` (it throws `RuntimeException` on failure, unlike the client
methods which return `null`) so a failed or stale pull still lets the local evaluation run. That
ordering also neutralises the same-second race with NSP's own job: whatever states the pull brings
over, the local evaluation immediately recomputes them from the window.

Trigger time is a fixed constant, not a `LiveConfig` field: it is a semantic boundary (the window is
defined in Berlin calendar days) rather than a tuning knob, and `LiveConfigService`'s
`^([01]\d|2[0-3]):[0-5]\d$` validation can't express seconds anyway. Flag
`${DAILY_RESULTS_SMART_LOCATION_ACTIVE_STATE_SCHEDULE_ON:true}` in `application.yml` — **the default
is what keeps CI green**: `verify-env-vars.yaml` only demands a Helm entry for a `${VAR}` with no
default, so zero Helm files are touched and none of the four checks can fail. Confirm with
`/check-vars cdr`.

Multi-replica locking is not needed: `replicaCount: 1`, `autoscaling` min=max=1.

### 4.4 Tests

Follow `DynamicSchedulerTest` / `LiveConfigServiceTest` (`MockitoAnnotations.openMocks`, SUT built
manually, `ReflectionTestUtils` for `@Value`, `method_shouldDoX_whenY`): cron/zone construction,
`buildDailyCronFromTime("00:00", 5)` → `"5 0 0 * * *"`, flag-off early return, and
evaluation-still-runs-when-pull-fails. Plus a `SmartLocationService` test for promote/demote and
save-if-changed. Any smoke test goes through the `/smoke-test` skill and its README matrix.

---

## 5. transit-dashboard

No Go changes — `internal/controllers/nsp_controller.go:79-93` forwards the request body verbatim
without parsing it, and the existing PATCH route already carries the new fields.

- `frontend/src/types/location.ts` — add `ACTIVE = "ACTIVE"` to the enum (L1-7) and
  `active_first_day?: string; active_last_day?: string;` to `SmartLocation` (L9-26). Optional,
  because NSP omits nulls entirely.
- `frontend/src/services/nsp.ts:104-128` — generalize to
  `patchSmartLocation(ids, payload: Partial<SmartLocation>)` and keep `patchLocationState` as a thin
  wrapper over it, so `NspLocationsPage.handleVerifyLocation` keeps working unchanged.
- `frontend/src/pages/navigation-service/NspLocationViewPage.tsx`, State Management card (L137-169):
  - add `ACTIVE` to the `<select>` (L151-162) — also required so that an already-ACTIVE location
    doesn't render a `<select>` with no matching `<option>`, which shows blank;
  - selecting any state **other than** `ACTIVE` PATCHes immediately, as today;
  - selecting `ACTIVE` does **not** PATCH the state — it reveals two `<input type="date">` fields
    plus a Save button (native date input is the dominant repo pattern, cf.
    `CdrAdapterTokensPage.tsx:191-208`);
  - Save PATCHes **only the two dates**, then re-fetches via `getLocation` and renders the state the
    server actually returned — which will already be `ACTIVE` when the window covers today, thanks to
    §3.3's write-time re-evaluation;
  - `validate()` per house convention (manual, returns bool, populates an `errors` record rendered as
    a red border + `<p className="mt-1 text-xs text-red-500">`): both days required, `first <= last`,
    ISO format via `parseIsoDay` from `utils/dateUtils.ts`;
  - a `useEffect` on `[locationData]` re-seeds both inputs after the re-fetch, so they never go stale;
  - copy in the card: *"ACTIVE is assigned automatically. A VERIFIED location becomes ACTIVE while
    today falls inside the window below (both days included, Europe/Berlin) and returns to VERIFIED
    once the window has passed. Only ACTIVE locations are served to other parties. Choosing another
    state clears the window."* Also say that a **future** window keeps the location VERIFIED until it
    opens — otherwise the save looks broken.
- `frontend/src/components/nsp/LocationsTable.tsx:23-42` — give `ACTIVE` its own badge
  (`bg-green-600 text-white`; VERIFIED already owns emerald, and a filled treatment reads as
  "stronger than VERIFIED").
- `generated/clients/nsp/nsp_client.gen.go` is stale but **harmless**: the BFF uses only the
  low-level `...WithBody` methods returning a raw `*http.Response` and never unmarshals into
  `nsp.SmartLocationDTO`. Regenerate via `scripts/generate-nsp-client.sh` for hygiene while NSP is
  already up (§4.1), but treat it as optional and non-blocking.

---

## 6. Verification

Services start/stop only via `./start.sh` / `./stop.sh`, in background, stopping first.

1. `install-local-javalibs.sh nsp cdr-adapter` → 4 × `Ok`.
2. Seed in `Nsp_*` via `mongosh --quiet "$LOCAL_MONGO_URI"` (after sourcing
   `transit-local-services/local/dev-config.sh`):
   - `LOC_IN` — `VERIFIED`, window `[today-1, today+1]` → must become `ACTIVE`
   - `LOC_EDGE` — `VERIFIED`, window `[today, today]` → must become `ACTIVE` (proves inclusivity)
   - `LOC_OUT` — `VERIFIED`, window `[today-10, today-5]` → must stay `VERIFIED`
   - `LOC_ARCH` — `ARCHIVED`, window covering today → must stay `ARCHIVED` (proves manual states win)
3. `POST localhost:8085/api/v1/internal/locations/refresh-active-states` → returns 2; re-query mongo
   and assert the four outcomes above plus `publish` tracking `ACTIVE`. Run it **again** → returns 0
   and `lastUpdated` must not move (proves idempotency + save-if-changed).
4. Demotion: move `LOC_IN`'s window into the past, re-run → back to `VERIFIED`, `publish:false`.
5. Sender endpoints: `GET .../ocpi/2.2.1/locations` lists only ACTIVE; `GET .../locations/DE/ABC/LOC_OUT`
   returns "Location not found". Also `PUT .../locations/DE/ABC/LOC_OUT/{evseUid}` must still succeed
   (proves the §3.4 internal-caller split).
6. Trigger wiring: start NSP with `ACTIVE_STATE_CHECK_CRON="0/20 * * * * *"`, watch for the refresh
   log line, then revert — proves `@Scheduled` + zone resolution without touching the clock.
7. cdr-adapter: `GET localhost:8085/api/v1/internal/locations` shows `"active_first_day": "2026-08-24"`
   as a **string**; `POST localhost:8082/api/v1/internal/smart-locations/update` must not log
   "No smart locations received"; `CdrAdapter_SmartLocations` must show
   `ISODate("...T00:00:00.000Z")` — **a `22:00:00Z` means the §1.3 converter is missing**; then its
   own refresh endpoint must reach the same states independently.
8. UI at `http://localhost:3094/nsp/locations/DE/ABC/ARCMINDLOC/view` — pick ACTIVE, set a window
   covering today, Save → flips to ACTIVE; future window → stays VERIFIED with the explanatory copy;
   switch to VERIFIED → both dates cleared; end date before start → red error, no request sent.
9. `./mvnw test` in openlib and cdr-adapter; `/check-vars cdr`; `./stop.sh` everything.

---

## 7. Risks / decisions to surface

1. **🔴 The checked-in OpenAPI spec is already missing `ACTIVE`** (§4.1) — the instant NSP serves it,
   cdr-adapter's pull becomes a silent no-op. Ship the two services together.
2. **🔴 `getLocationEvseConnector` has three internal callers** (§3.4) — gating it naively breaks
   OCPI `PUT`/`PATCH` on EVSEs and connectors for every non-ACTIVE location.
3. **🟠 Cutover blackout — needs a decision before any deploy.** No location has a window yet, so
   after this change `findActiveSmartLocations` returns **nothing** and roaming partners see an empty
   locations list. Either backfill a window for all currently-VERIFIED locations at deploy time, or
   accept a deliberate blackout. (An open-ended window — `activeLastDay = null` meaning "forever" —
   would make the backfill trivial, but contradicts the "both fields filled" rule; worth revisiting.)
4. **🟠 `publish` semantics move** from VERIFIED to ACTIVE, so every currently-VERIFIED location flips
   to `publish:false` on its next write. Consider a one-off backfill so touched and untouched
   documents don't disagree.
5. **🟠 cdr-adapter's existing jobs run in UTC, not Berlin** — likely a pre-existing latent bug. This
   plan deliberately leaves them alone; raise as a separate ticket.
6. **🟡 The generic PATCH can set a window but never clear it** — clearing happens only via the state
   transition (the user's chosen design). Document that in the controller's `@Operation`.
7. **🟡 DST is safe** at 00:00:05 Berlin (transitions happen at 02:00/03:00 local), so nobody should
   "helpfully" move the trigger to 02:30.
8. **🟡 Two schedulers, same second, no coordination** — safe only because both use the *same*
   `SmartLocationActivationUtil` from the *same* openlib. The 1.2.1/1.4.2 pins must move together
   forever; consider logging the openlib version at boot in both.
9. **🟢 No frontend test runner** and zero NSP Playwright coverage — the UI changes are hand-verified
   only. Say whether a first NSP `ui-it` spec is in scope.

---

## 8. Wrap-up

- Per `.ai/AGENTS.md`, copy this plan into `banula-nsp/plans/` (the repo with the most changes) and
  rename it descriptively, e.g. `smart-location-active-state-window.md`.
- Leave every repo **uncommitted** — no commits, no branches, no pushes.
- Send the completion notification once executed:
  `curl -d "Repo: banula-nsp | Plan: smart-location-active-state-window finished with <SUCCESS|DOUBT|FAILURE>" https://ntfy.sh/oli-dev-matheus-0551987`
- Report the full list of modified repositories at the end.
