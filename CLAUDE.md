# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
./gradlew :app:assembleDebug          # Build debug APK
./gradlew :app:testDebugUnitTest      # Run all unit tests
./gradlew :app:testDebugUnitTest --tests "com.dan.coroutinedojo.ExampleUnitTest.addition_isCorrect"  # Single test
./gradlew :app:connectedDebugAndroidTest  # Instrumented tests (requires emulator/device)
```

## Architecture

Single-module Android app (`app/`) teaching Kotlin coroutines through 5 progressive levels. Jetpack Compose UI, no navigation library — each level is a separate `ComponentActivity` launched via explicit intents from a data-driven launcher list in `MainActivity`.

### Level Structure

Each level activity follows the same pattern: **Objective → Antipattern → Best Practice → Explanation**, rendered through a shared `LessonScreen` composable (`ui/components/LessonScreen.kt`).

| Level | Topic | Status |
|-------|-------|--------|
| 1 | Dispatchers & `withContext` | Implemented |
| 2 | Lifecycle scopes (`lifecycleScope` vs `GlobalScope`) | Implemented |
| 3 | Structured concurrency (parent-child cancellation) | Implemented |
| 4 | StateFlow & SharedFlow | Stub only |
| 5 | Channels & Backpressure | Stub only |

### Key Dependencies

- **kotlinx-coroutines-android** / **kotlinx-coroutines-test** (1.10.2)
- **Compose BOM** 2024.09.00, Material3
- **Lifecycle** runtime-ktx, runtime-compose, viewmodel-ktx (2.9.1)
- Version catalog: `gradle/libs.versions.toml`

### Conventions

- Package: `com.dan.coroutinedojo`
- All level activities are registered in `AndroidManifest.xml` and listed in the `levels` data list in `MainActivity.kt`
- New levels: create `LevelNActivity`, add to `levels` list, register in manifest
- Coroutine tests use `runTest` + `TestDispatcher` from `kotlinx-coroutines-test`
- Unit tests at `app/src/test/java/`, instrumented tests at `app/src/androidTest/java/`
- `compileSdk 36`, `minSdk 28`, Java 11 source compatibility
