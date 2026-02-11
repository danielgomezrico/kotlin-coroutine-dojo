# Plan M3: Levels 4-5 + Unit Tests

## Pre-Planning Extract (from 1-investigation.md)
Files: Level4Activity.kt (stub), Level5Activity.kt (stub), ExampleUnitTest.kt (placeholder)
Patterns: LessonScreen composable, extract testable logic into standalone classes
Constraints: 1 antipattern + 1 best practice per level, unit tests with `runTest` + `TestDispatcher`
Deps: All required deps wired (coroutines-test, lifecycle-runtime-compose, lifecycle-viewmodel-ktx) ✅

## M3: Levels 4-5 + Unit Tests
┌─────────────────────────────────────────┐
│ VALUE: Reactive streams + backpressure  │
│         + test coverage                 │
│ PROGRESS: [█████████░] 90%             │
│ STATUS: Stubs only, needs full impl    │
└─────────────────────────────────────────┘

## Level 4 (StateFlow + SharedFlow) — Full Implementation

**Pitfall focus:** Non-lifecycle-aware Flow collection (the most impactful real-world bug).

### Design
1. **Create `FlowExamples.kt`** — standalone object/class with testable logic:
   - A `StateFlow<Int>` counter that increments every second
   - A function demonstrating StateFlow equality conflation (emit same value twice)
   - A `SharedFlow` with `replay=0` showing event loss for late subscribers
2. **Antipattern:** Collect a flow using `lifecycleScope.launch { flow.collect {} }` — show (via log/status text) that collection continues when app is backgrounded, wasting resources.
3. **Best practice:** Collect using `collectAsStateWithLifecycle()` in Compose — show that collection stops in background and restarts in foreground.
4. **Explanation covers:**
   - Why `StateFlow` silently drops duplicate emissions (equality-based conflation). Emit the same data class → collector doesn't fire. Workaround: wrapper with unique ID or `SharedFlow(replay=1)`.
   - Why `SharedFlow(replay=0)` loses events for late subscribers. One-shot events (navigation, snackbar) emitted before collector starts are gone. Channel is better for one-shot events.
   - Hot→cold after operators: `.map {}` on StateFlow returns a cold Flow; each collector runs its own operator chain. Re-share with `stateIn`/`shareIn`.

**Files to create/modify:**
- `app/src/main/java/com/dan/coroutinedojo/FlowExamples.kt` — new, testable logic
- `app/src/main/java/com/dan/coroutinedojo/Level4Activity.kt` — full implementation

## Level 5 (Channels + Backpressure) — Full Implementation

**Pitfall focus:** Forgetting to close a Channel (the most common real-world Channel bug).

### Design
1. **Create `ChannelExamples.kt`** — standalone object/class with testable logic:
   - A producer that sends items into a Channel without closing it
   - A producer using `produce {}` builder (auto-closes)
   - Buffered vs rendezvous vs conflated channel comparison
2. **Antipattern:** Producer sends N items into a `Channel()` (rendezvous) then finishes without calling `close()`. Consumer uses `for (item in channel)` and hangs forever after the last item. Show via status text: "Consumer waiting... (will never finish)".
3. **Best practice:** Same scenario using `produce {}` coroutine builder — channel auto-closes on completion, consumer terminates cleanly. Add `Channel.BUFFERED` to demonstrate non-blocking sends.
4. **Explanation covers:**
   - `send()` vs `trySend()` — `send` suspends when buffer full (can't call from non-suspending context like click handler). `trySend` returns immediately with success/failure. Use `trySend` + `CONFLATED` or `DROP_OLDEST` for UI events.
   - Rendezvous deadlocks — two coroutines each sending then receiving on opposite channels deadlock. Fix: add buffer.
   - Channel vs Flow guidance — prefer Flow for most stream use cases. Use Channel only for fan-out, fan-in, or true producer-consumer queues between independent coroutines.

**Files to create/modify:**
- `app/src/main/java/com/dan/coroutinedojo/ChannelExamples.kt` — new, testable logic
- `app/src/main/java/com/dan/coroutinedojo/Level5Activity.kt` — full implementation

## Unit Tests — New Test File

**Create `app/src/test/java/com/dan/coroutinedojo/CoroutineDojoTest.kt`** covering:

1. **Cancellation test:** Launch a coroutine in a `TestScope`, cancel the scope, verify the coroutine is cancelled via `isActive` / job state.
2. **Exception propagation test:** In `coroutineScope`, one child throws → verify sibling is cancelled. In `supervisorScope`, one child throws → verify sibling completes.
3. **StateFlow conflation test:** Emit same value twice to a `StateFlow` → verify collector only receives it once.
4. **Channel buffering test:**
   - `CONFLATED` channel: send 3 items without receiving → verify only last value is received.
   - Rendezvous channel: verify `send` suspends when no receiver.
5. **Channel close test:** Send items then close → verify `for` loop terminates.

All tests use `runTest` for virtual time control. No Android dependencies — pure Kotlin coroutine tests.

**Files to create:**
- `app/src/test/java/com/dan/coroutinedojo/CoroutineDojoTest.kt` — new

**Optionally remove:**
- `app/src/test/java/com/dan/coroutinedojo/ExampleUnitTest.kt` — placeholder

### Verification
- Expected I/O: Level 4 demonstrates lifecycle-aware vs unaware collection. Level 5 demonstrates channel close/buffer. All unit tests pass.
- Test cmd: `./gradlew :app:testDebugUnitTest`
- Value delivered: Advanced primitives with correct lifecycle handling + first real test coverage.

## Handoff
Execute M2 improvements first (Levels 1-2 explanation updates + Level 3 rework), then M3 (Levels 4-5 + tests). M2 is prerequisite because Level 3's exception handling concepts are referenced in Level 4 and 5 explanations.
