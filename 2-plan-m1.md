# Plan M1: Dependency + Launcher Baseline

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
