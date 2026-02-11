# Investigation: Coroutine Dojo — Pitfall Analysis Across All Milestones

## Summary
Audited all 3 milestones against the implemented code (Levels 1-3 done, 4-5 stubs) and cross-referenced with documented Kotlin coroutine learning pitfalls. Found significant gaps in the existing levels where critical pitfalls are not demonstrated, and identified concrete requirements for the stub levels. M1 is complete. M2 levels work but miss high-impact pitfalls. M3 needs full implementation with pitfall-aware design.

## Files
| File | Purpose | Relevance |
| --- | --- | --- |
| `app/src/main/java/com/dan/coroutinedojo/Level1Activity.kt` | Dispatchers lesson | Missing `delay` vs `Thread.sleep` and `suspend != background` pitfalls |
| `app/src/main/java/com/dan/coroutinedojo/Level2Activity.kt` | Lifecycle scopes lesson | Missing scope comparison table, `repeatOnLifecycle` mention |
| `app/src/main/java/com/dan/coroutinedojo/Level3Activity.kt` | Structured concurrency lesson | Missing async/await exceptions, SupervisorJob, CancellationException swallowing |
| `app/src/main/java/com/dan/coroutinedojo/Level4Activity.kt` | StateFlow/SharedFlow — **stub** | Needs full implementation |
| `app/src/main/java/com/dan/coroutinedojo/Level5Activity.kt` | Channels — **stub** | Needs full implementation |
| `app/src/main/java/com/dan/coroutinedojo/ui/components/LessonScreen.kt` | Shared lesson UI | Adequate for current format |
| `app/src/test/java/com/dan/coroutinedojo/ExampleUnitTest.kt` | Placeholder test | No coroutine tests exist |

## Patterns
- Lesson UI pattern: `LessonScreen` composable with Objective/Antipattern/BestPractice/Explanation sections @ `ui/components/LessonScreen.kt:25`
- Activity pattern: `ComponentActivity` → `enableEdgeToEdge()` → `setContent { Theme { Scaffold { ... } } }` — consistent across all levels
- State management: `remember { mutableStateOf() }` for UI status text, `rememberCoroutineScope()` for launching demos
- Launcher: data-driven `levels` list in `MainActivity.kt:33` drives both the list and manifest registration

## Constraints
- [x] Single module, Kotlin + Compose only
- [x] One activity per level; each level self-contained
- [x] LessonScreen accepts exactly one antipattern + one best practice per level
- [x] Unit tests only (`app/src/test/java/`)
- [x] compileSdk 36, minSdk 28, Java 11

## Deps
All required deps already wired in `gradle/libs.versions.toml`:
- `kotlinx-coroutines-android` 1.10.2 ✅
- `kotlinx-coroutines-test` 1.10.2 ✅
- `lifecycle-runtime-compose` 2.9.1 ✅
- `lifecycle-viewmodel-ktx` 2.9.1 ✅
- Compose BOM 2024.09.00, Material3 ✅

## Tests
Style: unit | Location: `app/src/test/java/com/dan/coroutinedojo/`
- Only `ExampleUnitTest` placeholder exists — **no coroutine tests**
- Test dep available: `kotlinx-coroutines-test` with `runTest`, `TestDispatcher`
- Needed: tests for cancellation, exception propagation, StateFlow conflation, Channel buffering

---

## Milestone-by-Milestone Pitfall Analysis

### M1: Dependency + Launcher Baseline ✅ COMPLETE
No issues. Deps wired, launcher functional, all activities declared.

### M2: Levels 1-3 — IMPLEMENTED BUT MISSING KEY PITFALLS

#### Level 1 (Dispatchers) — Gaps Found

**Current:** Shows `Thread.sleep` on Main (ANR) vs `withContext(Dispatchers.IO)`.

**Missing pitfalls:**

| Pitfall | Severity | Why it matters |
| --- | --- | --- |
| `delay()` vs `Thread.sleep()` inside a coroutine | High | Learners from threading backgrounds confuse suspension with blocking. `Thread.sleep` blocks the underlying thread for ALL coroutines on it; `delay` suspends only the current coroutine. This is the single most common beginner mistake. |
| `suspend` does NOT mean "runs on background thread" | High | RxJava developers assume `suspend` = `subscribeOn(IO)`. A suspend function runs on whatever dispatcher the caller uses unless it calls `withContext` internally. Convention: suspend funs should be main-safe. |

**Recommendation:** The current antipattern uses `Handler.postDelayed` + `Thread.sleep` which is convoluted. Replace or add a demo showing two `launch` blocks on Main: one using `Thread.sleep` (serialized, freezes both), one using `delay` (interleaved, UI stays responsive).

#### Level 2 (Lifecycle Scopes) — Gaps Found

**Current:** Shows `GlobalScope` (leaks) vs `lifecycleScope` (auto-cancelled).

**Missing pitfalls:**

| Pitfall | Severity | Why it matters |
| --- | --- | --- |
| `viewModelScope` vs `lifecycleScope` vs `rememberCoroutineScope` — wrong scope for the job | Medium | Learners don't know which scope to use when. `viewModelScope` survives rotation (data loading); `lifecycleScope` tied to Activity destroy (UI ops); `rememberCoroutineScope` tied to composition (Compose side-effects). |
| `repeatOnLifecycle` vs deprecated `launchWhenStarted` | Medium | `launchWhenStarted` only suspends collection (upstream keeps emitting/buffering). `repeatOnLifecycle` cancels and restarts. This is a battery/memory trap. |

**Recommendation:** Add a brief explanation section covering scope selection. The `repeatOnLifecycle` pitfall is better demonstrated in Level 4 (Flow collection), but should be foreshadowed here.

#### Level 3 (Structured Concurrency) — Significant Gaps

**Current:** Shows `GlobalScope` orphan child (survives parent cancel) vs nested `launch` (cancelled with parent).

**Plan said:** "async/await and error propagation vs SupervisorJob" — **not implemented**.

**Missing pitfalls:**

| Pitfall | Severity | Why it matters |
| --- | --- | --- |
| try-catch around `async` instead of `await()` | Critical | Exceptions from `async` propagate up the job hierarchy AND are rethrown at `await()`. Catching at the wrong site does nothing. |
| `SupervisorJob()` as builder argument (classic misuse) | Critical | `launch(SupervisorJob()) { ... }` does NOT make children supervised — the SupervisorJob becomes the parent of the outer launch, not of the inner children. Need `supervisorScope {}` instead. |
| `CoroutineExceptionHandler` on a child coroutine | High | Handler is silently ignored on children; only works on root scope. |
| Swallowing `CancellationException` via `catch(Exception)` or `runCatching` | Critical | Creates zombie coroutines that cannot be cancelled. The #1 subtle production bug. |
| Cooperative cancellation — CPU loops ignoring cancellation | High | `job.cancel()` does nothing until a suspension point. Need `ensureActive()` or `isActive` check. `Thread.sleep` is not a suspension point. |

**Recommendation:** Level 3 needs a major rework. The current demo only shows cancellation propagation, not exception handling or SupervisorJob — which are the hardest concepts. Consider splitting: keep current cancellation demo as-is but add exception handling as the best-practice section showing `supervisorScope` + proper try-catch on `await()`.

### M3: Levels 4-5 + Tests — NOT STARTED (stubs only)

#### Level 4 (StateFlow + SharedFlow) — Pitfalls to Address

| Pitfall | Severity | Teaching approach |
| --- | --- | --- |
| Collecting Flow without lifecycle awareness | Critical | Antipattern: `lifecycleScope.launch { flow.collect {} }` — continues in background. Best practice: `collectAsStateWithLifecycle()` in Compose. |
| StateFlow equality-based conflation | High | Emit same data class twice → second emission silently dropped. Confuses LiveData/RxJava devs. |
| SharedFlow `replay=0` loses events for late subscribers | High | One-shot events (navigation, snackbar) lost if emitted before collector starts. |
| Hot→cold after operators | Medium | `.map {}` on StateFlow returns cold Flow; each collector runs its own operator chain. |

**Recommendation:** Focus antipattern on non-lifecycle-aware collection (most impactful). Best practice shows `collectAsStateWithLifecycle`. Explanation covers conflation + replay pitfalls. Extract testable logic into a standalone class/object (not tied to Activity) so unit tests can cover StateFlow conflation and SharedFlow replay.

#### Level 5 (Channels + Backpressure) — Pitfalls to Address

| Pitfall | Severity | Teaching approach |
| --- | --- | --- |
| Forgetting to close a Channel | Critical | Antipattern: producer finishes without `close()` → consumer hangs forever. Best practice: `produce {}` builder auto-closes. |
| Rendezvous channel deadlocks | High | Two coroutines send-then-receive on opposite channels → deadlock. Fix: add buffer. |
| `send()` vs `trySend()` from non-suspending context | High | UI click handlers can't suspend; need `trySend()` with `CONFLATED` or `DROP_OLDEST`. |
| Channel vs Flow — when to choose which | Medium | Prefer Flow for most streams; Channel only for fan-out/fan-in or true queues. |

**Recommendation:** Focus antipattern on unclosed channel (most common real bug). Best practice shows `produce {}` with buffering. Explanation covers `send`/`trySend` and when to prefer Flow over Channel. Extract producer/consumer logic for unit tests.

### Unit Tests — Gap Across All Levels

No coroutine tests exist. Tests should cover:
1. **Cancellation:** Verify a scoped coroutine is cancelled when scope is cancelled
2. **Exception propagation:** Verify `supervisorScope` isolates child failures
3. **StateFlow conflation:** Verify duplicate emissions are dropped
4. **Channel buffering:** Verify `CONFLATED` drops old values; rendezvous suspends sender

All tests should use `runTest` + `TestDispatcher` for deterministic timing. Extract testable logic from Activities into plain Kotlin classes/objects.

---

## Risks
| Risk | Sev | Mitigation |
| --- | --- | --- |
| Level 3 teaches cancellation but not exception handling — the harder concept | Critical | Rework Level 3 to include `async`/`await` exception propagation and `supervisorScope` |
| `LessonScreen` only supports 1 antipattern + 1 best practice per level | Medium | Either extend `LessonScreen` or keep each level focused on one pitfall with explanation covering related ones |
| Testable logic is embedded in Activity lambdas — untestable | High | Extract coroutine logic into standalone functions/classes for M3 levels; refactor M2 levels if time allows |
| Level 1 antipattern uses `Handler.postDelayed` + `Thread.sleep` — convoluted | Low | Simplify to direct `Thread.sleep` call or reframe to show `delay` vs `Thread.sleep` |

## Recommendations
1. **Level 3 rework (M2):** Replace or augment the current demo to show exception handling with `async`/`await` + `supervisorScope`. The current cancellation demo is valuable but incomplete without the exception story.
2. **Level 4 implementation (M3):** Antipattern = non-lifecycle-aware `collect`. Best practice = `collectAsStateWithLifecycle`. Explanation covers conflation + replay. Extract a `CounterFlow` or similar testable class.
3. **Level 5 implementation (M3):** Antipattern = unclosed channel with hanging consumer. Best practice = `produce {}` builder with `Channel.BUFFERED`. Explanation covers `send` vs `trySend` and Channel vs Flow guidance.
4. **Unit tests (M3):** Create test file(s) covering cancellation, exception propagation, StateFlow conflation, Channel buffering using `runTest`.
5. **Level 1 minor improvement:** Add explanation text about `delay()` vs `Thread.sleep()` difference even if the demo isn't changed.
6. **Level 2 minor improvement:** Add explanation text about scope comparison (`viewModelScope` vs `lifecycleScope` vs `rememberCoroutineScope`).

## Verification
- [x] Files verified — all paths exist and code reviewed
- [x] Patterns confirmed — LessonScreen pattern consistent across levels
- [x] Constraints traced — single module, one activity per level, unit tests only
- [x] Deps mapped — all needed deps already in version catalog
- [ ] Tests validated — no coroutine tests exist (documented gap)

## Failures
| Issue | Action |
| --- | --- |
| Level 3 plan said "async/await + SupervisorJob" but implementation only shows cancellation | Documented as critical gap; recommended rework |
| No coroutine tests exist | Documented gap; M3 must include tests |
| `LessonScreen` limits each level to one antipattern/best-practice pair | Documented constraint; keep levels focused |

## Handoff
Planning should:
1. Update M2 plan to include Level 3 rework (exception handling + SupervisorJob)
2. Update M3 plan with specific pitfall-focused designs for Levels 4-5
3. Add testable class extraction as a requirement for Levels 4-5
4. Consider minor explanation additions to Levels 1-2 without changing their demos
