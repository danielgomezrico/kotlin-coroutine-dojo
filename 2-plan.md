# Plan: Coroutine Dojo Activity Plan Improvements

## Pre-Planning Extract (from 1-investigation.md)
Files: app/src/main/java/com/dan/coroutinedojo/MainActivity.kt, app/build.gradle.kts, gradle/libs.versions.toml, app/src/main/AndroidManifest.xml
Patterns: Compose `setContent` entry point, no coroutines/flow patterns yet
Constraints: Kotlin + Compose, one activity per level, unit tests only, include lifecycle-aware flow collection
Deps: kotlinx-coroutines-android, kotlinx-coroutines-test, lifecycle-runtime-compose, lifecycle-viewmodel-ktx
Risks: flow collection leaks without lifecycle-aware collection; missing coroutine test dispatcher control
Recommendations: add lifecycle-aware patterns (repeatOnLifecycle/collectAsStateWithLifecycle), add deps, add launcher, add unit tests

## MCP Validation
| Lib | Ver | Source | Status |
|-----|-----|--------|--------|
| kotlinx-coroutines | latest | context7 (/kotlin/kotlinx.coroutines) | ✅ |
| lifecycle-runtime-compose | - | - | ⚠️ UNVERIFIED |
| lifecycle-viewmodel-ktx | - | - | ⚠️ UNVERIFIED |

## Topic Order (senior dev new to coroutines)
1. Basic launch + dispatchers: `Main` vs `IO`, ANR risk, `withContext` boundaries.
2. Lifecycle + scopes: `lifecycleScope` vs custom `MainScope`, cancellation on destroy.
3. Structured concurrency: `coroutineScope`, `async/await`, failure propagation, `SupervisorJob`.
4. StateFlow + SharedFlow: hot streams, replay, UI collection with lifecycle.
5. Channels + backpressure: `Channel`, `trySend`, `buffer`, consumer speed control.

## M1: Dependency + Launcher Baseline
┌─────────────────────────────────────────┐
│ ANALOGY: Store (front desk to rooms)    │
│ [🏗️ Prev]═══[🚀 NEW]                   │
│ VALUE: Navigation + deps foundation     │
│ PROGRESS: [███░░░░░░░░] 30%             │
└─────────────────────────────────────────┘

Scope
- Add coroutines + lifecycle deps in gradle/libs.versions.toml and app/build.gradle.kts.
- Update MainActivity to show a launcher list to each level activity.
- Declare new activities in AndroidManifest.xml.

Interfaces
- MainActivity launches Level1..Level5 via explicit intents.
- Activity list is data-driven to avoid boilerplate.

### Verification
- Expected I/O: App builds, launcher shows 5 items, each opens its activity.
- Test cmd: `./gradlew :app:testDebugUnitTest`
- Value delivered: Entry point ready and dependencies wired.

## M2: Activities Level 1-3 (Dispatchers, Scopes, Structured Concurrency)
┌─────────────────────────────────────────┐
│ ANALOGY: Factory (lines, stations, QA)  │
│ [🏗️ Prev]═══[🚀 NEW]                   │
│ VALUE: Core coroutine mental model      │
│ PROGRESS: [██████░░░░] 60%             │
└─────────────────────────────────────────┘

Scope
- Level 1 Activity: objective + antipattern (blocking Main) + best practice (IO + Main) + explanation.
- Level 2 Activity: lifecycleScope vs manual scope cancellation, memory leak example.
- Level 3 Activity: structured concurrency with async/await and error propagation vs SupervisorJob.

Interfaces
- Each activity follows Objective/Antipattern/Best Practice/Explanation structure.
- Shared UI components for repeatable sections.

### Verification
- Expected I/O: Each activity renders sections and runs demo actions without ANR.
- Test cmd: `./gradlew :app:testDebugUnitTest`
- Value delivered: Progressive core concepts for a senior dev new to coroutines.

## M3: Activities Level 4-5 + Unit Tests
┌─────────────────────────────────────────┐
│ ANALOGY: Traffic system (signals/flow)  │
│ [🏗️ Prev]═══[🚀 NEW]                   │
│ VALUE: Reactive streams + backpressure  │
│ PROGRESS: [█████████░] 90%             │
└─────────────────────────────────────────┘

Scope
- Level 4 Activity: StateFlow + SharedFlow, collectAsStateWithLifecycle, repeatOnLifecycle demo.
- Level 5 Activity: Channel + backpressure, buffer vs rendezvous, trySend/drop.
- Unit tests: cancellation, structured concurrency failure, StateFlow replay, Channel buffering.

Interfaces
- Flow/Channel examples expose minimal public functions for tests.
- Use runTest + TestDispatcher for deterministic timing.

### Verification
- Expected I/O: Flow UI uses lifecycle-aware collection; tests pass with deterministic schedulers.
- Test cmd: `./gradlew :app:testDebugUnitTest`
- Value delivered: Advanced coroutine primitives with correct lifecycle handling.

## Handoff
- Proceed to `implementation` skill to execute M1→M2→M3.
- Confirm whether any additional lifecycle deps should be pinned to specific versions.
