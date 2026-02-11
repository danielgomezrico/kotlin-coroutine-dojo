# Investigation: Coroutine Dojo Activity Plan Improvements

## Summary
Reviewed the current Android project structure and dependencies, then cross-checked official Android guidance for coroutines, lifecycle-aware collection, and Flow usage. The plan needs explicit lifecycle-aware flow collection patterns, testing dependencies for coroutines/flows, and a small navigation entry point to reach each level activity without bloating a single screen.

## Files
| File | Purpose | Relevance |
| --- | --- | --- |
| app/src/main/java/com/dan/coroutinedojo/MainActivity.kt | Current Compose entry point | Need to evolve to a simple activity launcher list | 
| app/build.gradle.kts | App module dependencies | Missing coroutines + lifecycle compose + test deps |
| gradle/libs.versions.toml | Central dependency versions | Needs new coroutine + lifecycle compose + test entries |
| app/src/main/AndroidManifest.xml | Activity declarations | Will need additional activity entries |

## Patterns
- Compose entry point in [MainActivity.kt](file:///Users/danielgomez/others/CoroutineDojo/app/src/main/java/com/dan/coroutinedojo/MainActivity.kt) using `setContent` and `Scaffold`.
- No existing coroutine or flow patterns yet.

## Constraints
- Kotlin + Compose only; single module.
- One activity per level, 2-3 focused examples; avoid a monolithic screen.
- Unit tests only.

## Deps
- Module currently includes only `androidx.lifecycle:lifecycle-runtime-ktx` but not coroutines core or lifecycle compose.
- Recommended additions based on Android guidance:
  - `org.jetbrains.kotlinx:kotlinx-coroutines-android`
  - `org.jetbrains.kotlinx:kotlinx-coroutines-test` (unit tests)
  - `androidx.lifecycle:lifecycle-runtime-compose` (Compose `collectAsStateWithLifecycle`)
  - `androidx.lifecycle:lifecycle-viewmodel-ktx` if ViewModel scope examples are included

## Tests
Style: unit | Location: app/src/test/java/
- No coroutine tests exist; we will add unit tests using `runTest`, `TestDispatcher`, and Flow testing patterns.

## Risks
| Risk | Sev | Mitigation |
| --- | --- | --- |
| Flow collection leaks if not lifecycle-aware | High | Use `repeatOnLifecycle` and/or `collectAsStateWithLifecycle` per official docs |
| Missing coroutine test dispatcher control | Medium | Add `kotlinx-coroutines-test` and use `runTest` |
| Activity explosion without navigation | Low | Add a small launcher list in MainActivity |

## Recommendations
1. Add lifecycle-aware Flow collection patterns (`repeatOnLifecycle`, `collectAsStateWithLifecycle`) explicitly in Level 4 activity.
2. Include at least one example highlighting `repeatOnLifecycle` vs `launch` collecting (per Android docs warning).
3. Add coroutine + test dependencies in `libs.versions.toml` and `app/build.gradle.kts`.
4. Provide a minimal `MainActivity` launcher screen to navigate to each level activity.
5. Unit tests should cover cancellation, structured concurrency failure propagation, StateFlow replay, and Channel backpressure behavior using `runTest`.

## Sources
- Android coroutines with lifecycle-aware components: https://developer.android.com/topic/libraries/architecture/coroutines
- StateFlow and SharedFlow guidance: https://developer.android.com/kotlin/flow/stateflow-and-sharedflow

## Verification
- [x] Files verified
- [x] Patterns confirmed
- [x] Constraints traced
- [x] Deps mapped
- [ ] Tests validated (pending)

## Failures
| Issue | Action |
| --- | --- |
| No existing coroutine tests | Documented gap |

## Handoff
Planning should incorporate lifecycle-aware collection as a first-class requirement and add missing coroutine/testing dependencies before implementation.
