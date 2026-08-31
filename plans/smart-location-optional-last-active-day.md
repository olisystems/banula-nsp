# SmartLocation activation window: optional last day, auto-ARCHIVED, date-only DailyResult gate

## Context

Today a SmartLocation's activation window only exists when **both** `active_first_day` and
`active_last_day` are set. A null last day means "no window at all", so the location can never be
ACTIVE — and an already-ACTIVE one is demoted to VERIFIED on the next nightly run. The dashboard
enforces the same thing client-side ("Last day is required"), so an open-ended window is
unreachable from the UI.

Three further gaps come with it:

- `ARCHIVED` exists in `SmartLocationState` but is **dead code** — the refresh job never assigns it
  and never leaves it. A window whose end has passed silently falls back to VERIFIED.
- There is **no server-side check** that the last day is not before the first. An inverted window is
  accepted, persisted, and merely logs a warning on every evaluation. Only the dashboard checks it.
- Nothing can be cleared through NSP's PATCH: `ModelPatcherUtil` copies non-null values only. The one
  escape hatch does the opposite of what's wanted — sending `smart_location_state` wipes **both**
  window days.

Separately, cdr-adapter today re-computes SmartLocation states locally (a second copy of NSP's
logic), while its DailyResult pipeline ignores `smart_location_state` entirely and gates only on
"does this location exist in the mirror" — which contradicts its own README.

**Intended outcome**

1. `active_last_day` is optional. First day set + no last day = open-ended window; the location is
   ACTIVE from the first day onward.
2. When the window has passed (both dates in the past), NSP sets the state to **ARCHIVED**
   automatically. Clearing the last day again revives the record to ACTIVE when the first day is
   today or earlier — so a mistakenly-entered last day is fully reversible.
3. Dates stay editable on ACTIVE and ARCHIVED records, and NSP rejects an inverted window (the check
   the dashboard already has).
4. cdr-adapter **never changes SmartLocation state**. Its scheduler only pulls from NSP, and
   DailyResult eligibility is decided purely from the two dates, ignoring the state.

## Decisions taken (confirmed with the developer)

- **Shared logic goes in `banula-open-library`**, released through the openlib → private-lib chain so
  NSP and the cdr-adapter mirror stay bit-identical.
- **Clearing a date uses an explicit JSON `null`** on the existing NSP PATCH endpoint; no new
  endpoint.
- **cdr-adapter loses all state mutation** (PO decision) and gates DailyResults on the dates only.

---

## Part 1 — `banula-open-library` (v1.2.2 → 1.2.3)

### `src/main/java/com/banula/openlib/ocpi/custom/smartlocations/util/SmartLocationActivationUtil.java`

This is the single source of truth both services call. Rework it so a window needs only a first day:

- `hasActivationWindow(location)` — now true when **`activeFirstDay != null`** (last day optional).
- `isWindowValid(location)` — valid when `activeLastDay == null || !first.isAfter(last)`.
- **New primitive** `isWithinActiveWindow(LocalDate first, LocalDate last, LocalDate reference)` —
  the date-only form, so cdr-adapter can call it with a `SmartLocationDTO`'s fields (the existing
  `isWithinActiveWindow(SmartLocation, LocalDate)` delegates to it):
  - `first == null || reference == null` → false
  - inverted window (`last != null && first.isAfter(last)`) → false, keep the existing `log.warn`
  - `reference.isBefore(first)` → false
  - else → `last == null || !reference.isAfter(last)`
- **New** `isWindowPassed(LocalDate first, LocalDate last, LocalDate reference)` →
  `first != null && last != null && reference.isAfter(last)`.
- `resolveState(location, today)` — widen the movable set to **`{VERIFIED, ACTIVE, ARCHIVED}`**
  (`PLAIN_OCPI`, `ENRICHED`, `INVALID` and `null` still win and are returned untouched), then:

  ```
  if (!hasActivationWindow) return current;      // no first day → manual decision stands
  if (!isWindowValid)       return current;      // inverted → never move (already warns)
  if (isWithinActiveWindow) return ACTIVE;
  if (isWindowPassed)       return ARCHIVED;     // ← the new rule
  return VERIFIED;                               // window still in the future
  ```

- `applyActiveState` and `isPubliclyServable` are unchanged.

Update the class javadoc: ARCHIVED is no longer described as a purely manual state.

### Tests — `src/test/java/.../SmartLocationActivationUtilTest.java`

Extend the existing pinned-date truth table (`TODAY = 2026-08-24`) with: open-ended window active;
open-ended window not yet started; passed window → ARCHIVED; ARCHIVED + cleared last day + past
first day → ACTIVE (the developer's reversibility scenario); manual ARCHIVED with no window stays
ARCHIVED; inverted window still never moves.

### Release

`pom.xml` → `1.2.3`. Build/install locally first (`./mvnw install`) so NSP and cdr-adapter compile
against it, and only run the actual Central deploy via `/deploy-java-libs` after confirming the
version.

## Part 2 — `private-lib` (v1.4.2 → 1.4.3)

`pom.xml`: bump `banula-open-library` to `1.2.3` and its own version to `1.4.3`. No code change —
it only needs to re-expose the new openlib to cdr-adapter. Deploy as the second link of the chain.

## Part 3 — `banula-nsp`

### `pom.xml`
`banula-open-library` `1.2.1` → `1.2.3`.

### `controller/nonocpi/NonOcpiSmartLocationController.java`

`patchSmartLocation` must distinguish "key absent" (leave unchanged) from `"active_last_day": null`
(clear it). Bind the body as `JsonNode`, then:

- convert to `SmartLocationDTO` with the injected `ObjectMapper`;
- compute `boolean clearFirstDay = body.has("active_first_day") && body.get("active_first_day").isNull()`
  and the same for `active_last_day`;
- pass both flags to the service.

Keep the endpoint path, the `POST` alias behaviour and the OCPI response wrapper as they are. Update
the `@Operation` description, which currently states the window can never be cleared by sending
nulls.

### `service/NSPSmartLocationService(Impl).java`

Add an overload `patchSmartLocation(countryCode, partyId, id, dto, clearFirstDay, clearLastDay)`;
the existing 4-arg method delegates with `false, false` so the **CSV bulk import path is untouched**.
Inside `patchSmartLocation`, keeping the current order of operations:

1. Keep the existing rejection of a manually-set `ACTIVE`.
2. After `ModelPatcherUtil.smartLocationPatcher(...)`, apply the explicit clears
   (`setActiveFirstDay(null)` / `setActiveLastDay(null)`).
3. **Change the wipe-on-state-change hack**: only null both days when a state was requested **and**
   the payload carried no window key. Today it fires unconditionally, which would make it impossible
   to edit dates on an ACTIVE/ARCHIVED record in one call.
4. **New validation**, thrown as `OCPICustomException` with
   `Constants.STATUS_CODE_INVALID_OR_MISSING_PARAMETERS`, evaluated on the *resulting* entity:
   - both days present and `first.isAfter(last)` → *"active_last_day must not be before
     active_first_day"*;
   - `activeLastDay` present while `activeFirstDay` is null → *"active_last_day requires
     active_first_day"* (a last-only window is meaningless under the new rule).
   Equal days stay legal — the existing single-day-window test depends on it.
5. `evaluateAndSave(...)` is unchanged and picks the new ARCHIVED outcome up for free.

In `refreshActiveStates()`, widen the candidate query to
`List.of(VERIFIED, ACTIVE, ARCHIVED)` so archived records can be revived by the nightly job.

### Tests — `src/test/java/.../NSPSmartLocationServiceImplTest.java`

Add: open-ended window activates; passed window archives; explicit null clears the last day and
revives ARCHIVED → ACTIVE; inverted window is rejected; last-day-without-first is rejected; dates are
still editable on an ACTIVE and on an ARCHIVED record; a state change with no window key still clears
the window (regression on existing behaviour).

## Part 4 — `banula-cdr-adapter`

### `pom.xml`
`private-library` `1.4.2` → `1.4.3`.

### Remove all local state mutation

- `service/SmartLocationService.java` — delete both `refreshActiveStates()` overloads and the
  `publish` / `lastUpdated` / `save` writes they performed. `pullSmartLocations()` stays exactly as
  it is (it already mirrors state verbatim from NSP under `mirrorLock`).
- `controller/nonocpi/NonOcpiSmartLocationsController.java` — delete `POST /refresh-active-states`
  (nothing in the dashboard calls it; the page's button uses `POST /smart-locations/update`).
- `repository/SmartLocationRepository.java` — drop `findBySmartLocationStateIn` once unused.
- `src/test/java/.../SmartLocationServiceActiveStateTest.java` — delete; replaced by the eligibility
  tests below.

### `schedulers/DynamicScheduler.java`

Rename `refreshSmartLocationActiveStates()` → `updateSmartLocationsFromNsp()` (and the matching
`ScheduledFuture` field at L77 / registration at L129-130). The body reduces to the config gate plus
`smartLocationService.pullSmartLocations()` in its try/catch — i.e. exactly what the dashboard button
on `/cdr-adapter/smart-locations` triggers. **Keep the existing config keys**
(`daily-results.smart-location-active-state-schedule-on`, `...-update-trigger-time`) so no Helm
values change in dev/int/prod and `/check-vars` stays green; update their comments and the README
wording instead. Update `DynamicSchedulerTest` (the pull-then-evaluate `InOrder` assertions no longer
apply).

### `service/DailyResultService.java` — the new date-only gate

Add one private helper and use it at the single choke point:

```java
private boolean isEligibleForDailyResult(SmartLocationDTO location, LocalDate day) {
    return SmartLocationActivationUtil.isWithinActiveWindow(
            location.getActiveFirstDay(), location.getActiveLastDay(), day);
}
```

`smart_location_state` is **not** consulted anywhere in this path.

- `updateDailyResultByDayAndLocation(...)` (L1728, right after the existing
  `smartLocationDTO == null` guard) — return a `MessageStatus.WARNING` `ControllerMessage` when the
  location is not eligible. This is the choke point: it covers `createDailyResultsByDay`,
  `findOrCreateDailyResult` (the BANULA/non-BANULA classification for sessions and CDRs) and the
  explicit regenerate endpoint.
- `createDailyResultsByDay(LocalDate day)` (L122-140) — also filter the list with the same helper so
  the returned count reflects only the locations actually processed.

**Reference date:** the helper takes the DailyResult's `day`, not `LocalDate.now()`. For the nightly
run they coincide, and for backfills/regeneration it correctly asks "was this location active on
*that* day" rather than "is it active today" — otherwise archiving a location would make its
historical days unregenerable. Flagging this explicitly as the one place where the literal wording
("today") was interpreted; say so if you want `now()` instead.

### Smoke tests — required, or they break

`SmokeTests/smoke/fixtures/smart_location_SMOKETEST_LOC_A.json` and
`smart_location_SMOKETEST_LOC_NM.json` are `"smart_location_state": "ACTIVE"` with **no
`active_first_day`/`active_last_day` at all**. Under the new gate they become ineligible and every
DailyResult smoke test fails. Add `"active_first_day"` set to a fixed past date (leave
`active_last_day` absent → open-ended), which also makes them the first fixtures exercising the
open-ended window.

Add a Java unit test class (e.g. `DailyResultServiceLocationWindowTest`) covering: open-ended window
with a past first day → eligible; today inside a closed window → eligible; both edges inclusive;
window passed → not eligible; window not yet started → not eligible; no first day → not eligible;
**ARCHIVED with a covering window → eligible** and **ACTIVE with a passed window → not eligible**
(the two cases that prove the state is ignored).

### `README.md`

Update the Glossary (lines 45-52) and Daily Trigger (568-578) sections: the location half of the
BANULA test is now a real date check rather than mere existence, and the daily job pulls from NSP
without touching state. Document `active_first_day`/`active_last_day` under "Smart Location
Extensions" (671-676), where they are currently missing.

## Part 5 — `transit-dashboard`

No Go BFF changes — it streams the request body untouched, and the pure-proxy rule in its
`.ai/AGENTS.md` forbids logic there.

### `frontend/src/types/location.ts`
Widen to `active_last_day?: string | null` (L29) so an explicit null can be sent. `active_first_day`
stays `string | undefined`.

### `frontend/src/components/shared/SmartLocationStatePanel.tsx`
- `validateWindow` (L89-110): drop the unconditional *"Last day is required"* (L98-102). Keep "First
  day is required". Run the ordering check (L104) **only when both values are non-empty**; keep the
  `>` comparison so equal days remain a valid one-day window.
- `handleSaveWindow` (L122-125): send `active_last_day: activeLastDay || null` — an explicit `null`,
  never `""`, which is what the new NSP branch keys off.
- Success messaging (L127-139): add an ARCHIVED branch ("the window has passed, the location was
  archived").
- Explanatory copy (L220-226 and L286-292): describe the open-ended window and that a passed window
  archives automatically and is reversible by clearing the last day.
- Leave `ARCHIVED` selectable in the state dropdown — a manual archive still clears the window and
  is sticky, which coexists with the derived one.

### Optional
`ui-it/tests/nsp-smart-location-window.spec.ts` — the panel already exposes stable ids
(`#activeFirstDay`, `#activeLastDay`, `#smartLocationState`) for a regression test of the
set-last-day → ARCHIVED → clear-last-day → ACTIVE round trip.

---

## Risks to confirm before/while rolling out

1. **Existing data with no `active_first_day`.** In cdr-adapter these locations stop producing
   DailyResults and MSCONS output. This follows directly from the stated rule ("otherwise not
   considered"), but it is the one change that can silently stop production output — worth a
   `mongosh` count of `CdrAdapter_SmartLocations` documents lacking `activeFirstDay` in dev before
   merging.
2. **Existing VERIFIED/ACTIVE locations with a passed window** flip to ARCHIVED on NSP's first
   nightly run after deploy. Expected, but visible to roaming partners via `publish`.
3. **Release ordering** is strict: openlib 1.2.3 → private-lib 1.4.3 → NSP + cdr-adapter poms.
   cdr-adapter does not depend on openlib directly.

## Verification

Build and unit tests (all four repos):

```bash
cd banula-open-library && ./mvnw test && ./mvnw install
cd ../private-lib        && ./mvnw install
cd ../banula-nsp         && ./mvnw test
cd ../banula-cdr-adapter && ./mvnw test
cd ../transit-dashboard/frontend && npm run build && npm run lint
```

End-to-end through the UI (`./stop.sh` then `./start.sh` in background for nsp, cdr, dashboard; then
`./scripts/health-check.sh nsp|cdr|dashboard`), driving the Vite dev server per the dashboard rules:

1. `http://localhost:3094/nsp/locations/<cc>/<party>/<id>/edit` — set first day in the past, leave
   last day blank → save → state becomes **ACTIVE**.
2. Set the last day to yesterday → save → state becomes **ARCHIVED** (the "mistake" case).
3. Clear the last day → save → state returns to **ACTIVE** (the reversibility requirement).
4. Set the last day before the first day → save → NSP rejects with the new message, and the panel
   also blocks it client-side.
5. `http://localhost:3094/cdr-adapter/smart-locations` — press the update button, confirm the mirror
   matches NSP and that **no cdr-adapter-side state change** happens.
6. Regenerate a DailyResult for a day inside the window (created) and for a day after the window
   (skipped with the WARNING message).

Direct checks:

```bash
source transit-local-services/local/dev-config.sh
mongosh --quiet "$LOCAL_MONGO_URI" --eval 'db.Nsp_Location.find({}, {id:1, smartLocationState:1, activeFirstDay:1, activeLastDay:1}).toArray()'
curl -s -X PATCH localhost:8085/api/v1/internal/locations/DE/CST/<id> \
  -H 'Content-Type: application/json' -d '{"active_last_day": null}'
```

Then the cdr-adapter smoke suite (`SmokeTests`) with the updated fixtures.

## Repos modified

`banula-open-library`, `private-lib`, `banula-nsp`, `banula-cdr-adapter`, `transit-dashboard` — all
three service repos are already on `fix/OLISYS-4890/last_active_day_null`; openlib and private-lib
are on `main` and, per the repo rules, are changed in place and left uncommitted.

Per `.ai/AGENTS.md`, this plan file will be copied to `banula-nsp/plans/` on implementation (it owns
the core rule change); say if you'd rather it live in `banula-cdr-adapter/plans/`.
