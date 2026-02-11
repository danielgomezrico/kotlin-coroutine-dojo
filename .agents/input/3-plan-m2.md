# Plan M2: Levels 1-3 — Pitfall-Aware Improvements

## Pre-Planning Extract (from 1-investigation.md)
Files: Level1Activity.kt, Level2Activity.kt, Level3Activity.kt, LessonScreen.kt
Patterns: LessonScreen composable (Objective/Antipattern/BestPractice/Explanation), one activity per level
Constraints: LessonScreen supports 1 antipattern + 1 best practice per level; use explanation text for additional pitfalls
Deps: All required deps already wired ✅

## M2: Pitfall-Aware Improvements to Levels 1-3
┌─────────────────────────────────────────┐
│ VALUE: Close critical learning gaps     │
│ PROGRESS: [██████░░░░] 60%             │
│ STATUS: Levels exist but miss key       │
│         pitfalls identified in audit    │
└─────────────────────────────────────────┘

## Level 1 (Dispatchers) — Minor Updates

**Current state:** Antipattern = `Thread.sleep` on Main via Handler; Best practice = `withContext(Dispatchers.IO)`. Works but misses foundational pitfalls.

**Changes:**
1. **Update explanation text** to cover:
   - `delay()` vs `Thread.sleep()` — suspension vs blocking. `Thread.sleep` blocks the thread for ALL coroutines on it; `delay` only suspends the current one.
   - `suspend` does NOT mean "runs on background thread" — a suspend function runs on whatever dispatcher the caller uses. Convention: suspend funs should be main-safe (handle their own `withContext`).
2. **No demo changes** — keep antipattern/best-practice demos as-is; explanation text is sufficient for these concepts.

**Files to modify:**
- `app/src/main/java/com/dan/coroutinedojo/Level1Activity.kt` — update `explanation` string

## Level 2 (Lifecycle Scopes) — Minor Updates

**Current state:** Antipattern = `GlobalScope`; Best practice = `lifecycleScope`. Works but lacks scope selection guidance.

**Changes:**
1. **Update explanation text** to add scope comparison:
   - `viewModelScope` — survives config changes, use for data loading / business logic
   - `lifecycleScope` — tied to Activity/Fragment destroy, use for UI operations
   - `rememberCoroutineScope` — tied to Compose composition, use for interaction-driven side-effects
2. **Foreshadow `repeatOnLifecycle`** — brief mention that lifecycle-aware Flow collection (Level 4) builds on this concept.
3. **No demo changes** — GlobalScope vs lifecycleScope demo is effective as-is.

**Files to modify:**
- `app/src/main/java/com/dan/coroutinedojo/Level2Activity.kt` — update `explanation` string

## Level 3 (Structured Concurrency) — Major Rework

**Current state:** Shows GlobalScope orphan (survives parent cancel) vs nested `launch` (cancelled with parent). The original plan called for async/await + SupervisorJob but this was never implemented.

**Problem:** Cancellation propagation is only half the story. Exception handling with `async`/`await` and `supervisorScope` are the hardest and most error-prone structured concurrency concepts.

**Changes:**
1. **Replace antipattern** — Show `async` exception mishandling:
   - Launch two `async` tasks in a regular `coroutineScope`
   - One fails with an exception
   - The other gets cancelled (collateral damage from structured concurrency)
   - Demonstrate that try-catch around `async {}` (not `await()`) does nothing
2. **Replace best practice** — Show `supervisorScope` isolation:
   - Same two `async` tasks inside `supervisorScope`
   - First fails, second continues unaffected
   - Proper try-catch around `await()` handles the failure
3. **Update explanation** to cover:
   - Why `SupervisorJob()` as a builder argument is a classic misuse (it becomes parent of outer launch, not inner children)
   - `CancellationException` swallowing — `catch(Exception)` or `runCatching` creates zombie coroutines
   - Cooperative cancellation — `ensureActive()` / `isActive` for CPU-bound loops
   - Brief mention: `CoroutineExceptionHandler` only works on root scope, not children

**Files to modify:**
- `app/src/main/java/com/dan/coroutinedojo/Level3Activity.kt` — full rewrite of antipattern + best practice + explanation

### Verification
- Expected I/O: Level 1-2 show updated explanations. Level 3 demonstrates exception propagation vs supervisorScope isolation.
- Test cmd: `./gradlew :app:testDebugUnitTest`
- Value delivered: Closes the biggest learning gaps — exception handling and supervisorScope are the concepts most devs get wrong.
