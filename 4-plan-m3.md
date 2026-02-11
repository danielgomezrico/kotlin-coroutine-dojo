# Plan M3: Activities Level 4-5 + Unit Tests

## Pre-Planning Extract (from 1-investigation.md)
Files: app/src/main/java/com/dan/coroutinedojo/MainActivity.kt, app/build.gradle.kts, gradle/libs.versions.toml, app/src/main/AndroidManifest.xml
Patterns: Compose `setContent` entry point, no coroutines/flow patterns yet
Constraints: Kotlin + Compose, one activity per level, unit tests only, include lifecycle-aware flow collection
Deps: kotlinx-coroutines-android, kotlinx-coroutines-test, lifecycle-runtime-compose, lifecycle-viewmodel-ktx
Risks: flow collection leaks without lifecycle-aware collection; missing coroutine test dispatcher control
Recommendations: add lifecycle-aware patterns (repeatOnLifecycle/collectAsStateWithLifecycle), add deps, add launcher

## MCP Validation
| Lib | Ver | Source | Status |
|-----|-----|--------|
| kotlinx-coroutines | latest | context7 (/kotlin/kotlinx.coroutines) | ✅ |
| lifecycle-runtime-compose | - | - | ⚠️ UNVERIFIED |
| lifecycle-viewmodel-ktx | - | - | ⚠️ UNVERIFIED |

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
