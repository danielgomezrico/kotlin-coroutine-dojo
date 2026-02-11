# Plan M2: Activities Level 1-3 (Dispatchers, Scopes, Structured Concurrency)

## Pre-Planning Extract (from 1-investigation.md)
Files: app/src/main/java/com/dan/coroutinedojo/MainActivity.kt, app/build.gradle.kts, gradle/libs.versions.toml, app/src/main/AndroidManifest.xml
Patterns: Compose `setContent` entry point, no coroutines/flow patterns yet
Constraints: Kotlin + Compose, one activity per level, unit tests only, include lifecycle-aware flow collection
Deps: kotlinx-coroutines-android, kotlinx-coroutines-test, lifecycle-runtime-compose, lifecycle-viewmodel-ktx
Risks: flow collection leaks without lifecycle-aware collection; missing coroutine test dispatcher control
Recommendations: add lifecycle-aware patterns (repeatOnLifecycle/collectAsStateWithLifecycle), add deps, add launcher

## MCP Validation
| Lib | Ver | Source | Status |
|-----|-----|--------|--------|
| kotlinx-coroutines | latest | context7 (/kotlin/kotlinx.coroutines) | ✅ |
| lifecycle-runtime-compose | - | - | ⚠️ UNVERIFIED |
| lifecycle-viewmodel-ktx | - | - | ⚠️ UNVERIFIED |

## M2: Activities Level 1-3
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
