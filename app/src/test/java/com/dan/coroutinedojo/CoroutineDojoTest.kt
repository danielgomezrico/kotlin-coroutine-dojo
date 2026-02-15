package com.dan.coroutinedojo

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineDojoTest {

    // =====================================================================
    // Structured Concurrency — coroutineScope vs supervisorScope
    // =====================================================================

    // ANTIPATTERN: coroutineScope cancels ALL children when one fails.
    // This is wrong when tasks are independent — the healthy sibling dies too.
    @Test
    fun `ANTIPATTERN - coroutineScope kills healthy sibling when one child fails`() = runTest {
        var siblingCancelled = false
        val result = runCatching {
            coroutineScope {
                launch {
                    try {
                        delay(Long.MAX_VALUE) // healthy sibling doing work
                    } finally {
                        siblingCancelled = true
                    }
                }
                launch {
                    delay(100)
                    throw RuntimeException("child failed")
                }
            }
        }
        assertTrue("coroutineScope should propagate the failure", result.isFailure)
        assertTrue("Healthy sibling was cancelled as collateral damage", siblingCancelled)
    }

    // BEST PRACTICE: supervisorScope lets each child fail independently.
    @Test
    fun `BEST PRACTICE - supervisorScope isolates child failures`() = runTest {
        var siblingCompleted = false
        supervisorScope {
            val failing = async {
                delay(100)
                throw RuntimeException("child failed")
            }
            val succeeding = launch {
                delay(200)
                siblingCompleted = true
            }
            runCatching { failing.await() }
            succeeding.join()
        }
        assertTrue("Sibling completes despite other child failing", siblingCompleted)
    }

    // =====================================================================
    // Cancellation — swallowing vs rethrowing CancellationException
    // =====================================================================

    // ANTIPATTERN: catch(Exception) swallows CancellationException.
    // The coroutine refuses to cancel — it becomes a zombie.
    @Test
    fun `ANTIPATTERN - swallowing CancellationException creates zombie coroutine`() = runTest {
        var zombieIterations = 0
        val job = launch {
            while (true) {
                zombieIterations++
                try {
                    delay(1)
                } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                    // BAD: catches CancellationException along with other exceptions.
                    // In production this loop would keep running forever after cancel().
                    // We break here only to keep the test finite.
                    break
                }
            }
        }
        yield()
        job.cancel()
        job.join()
        assertTrue("Coroutine ran and caught cancellation instead of dying", zombieIterations >= 1)
    }

    // BEST PRACTICE: always rethrow CancellationException after cleanup.
    @Test
    fun `BEST PRACTICE - rethrowing CancellationException allows clean shutdown`() = runTest {
        var cleanupRan = false
        val job = launch {
            try {
                delay(Long.MAX_VALUE)
            } catch (e: Exception) {
                if (e is CancellationException) {
                    cleanupRan = true // do cleanup
                    throw e           // then rethrow — coroutine cancels properly
                }
            }
        }
        yield()
        job.cancel()
        job.join()
        assertTrue("Cleanup ran before cancellation completed", cleanupRan)
        assertTrue("Job is properly cancelled", job.isCancelled)
    }

    // =====================================================================
    // Cooperative Cancellation — CPU-bound loops
    // =====================================================================

    @Test
    fun `cancelling scope cancels child coroutine`() = runTest {
        val job = launch {
            delay(Long.MAX_VALUE)
        }
        assertTrue("Job should be active before cancel", job.isActive)
        job.cancel()
        job.join()
        assertTrue("Job should be cancelled after cancel", job.isCancelled)
        assertFalse("Job should not be active after cancel", job.isActive)
    }

    // BEST PRACTICE: ensureActive() in CPU-bound loops cooperates with cancellation.
    //
    // The antipattern: a tight loop like `while(true) { /* CPU work */ }` has no
    // suspension points, so cancel() is silently ignored and the coroutine becomes
    // unkillable. (This can't be unit-tested safely — the loop would hang the test.)
    //
    // The fix: call ensureActive() between iterations. It throws CancellationException
    // when the coroutine has been cancelled, giving the same cooperative behavior that
    // suspend functions like delay() provide automatically.
    @Test
    fun `BEST PRACTICE - ensureActive in loops throws on cancellation`() = runTest {
        var ensureActiveThrew = false
        val job = launch {
            try {
                delay(Long.MAX_VALUE)
            } catch (e: CancellationException) {
                // After cancellation, ensureActive() immediately throws
                ensureActiveThrew = runCatching { ensureActive() }.isFailure
                throw e
            }
        }
        yield()
        job.cancel()
        job.join()
        assertTrue("ensureActive() throws CancellationException on cancelled coroutine", ensureActiveThrew)
    }

    // =====================================================================
    // StateFlow — duplicate emissions
    // =====================================================================

    @Test
    fun `StateFlow drops duplicate emissions`() = runTest {
        val stateFlow = MutableStateFlow(0)
        val collected = mutableListOf<Int>()
        val job = launch {
            stateFlow.collect { collected.add(it) }
        }
        yield()
        stateFlow.value = 1
        yield()
        stateFlow.value = 1 // duplicate — should be dropped
        yield()
        stateFlow.value = 2
        yield()
        job.cancel()
        assertEquals("Duplicate value should be dropped", listOf(0, 1, 2), collected)
    }

    // =====================================================================
    // Channels — closing and lifecycle
    // =====================================================================

    // ANTIPATTERN: producer finishes without close() — consumer hangs forever.
    @Test
    fun `ANTIPATTERN - unclosed channel causes consumer to hang indefinitely`() = runTest {
        val channel = Channel<Int>()
        launch {
            for (i in 1..3) {
                channel.send(i)
            }
            // BUG: forgot channel.close()
        }
        val received = mutableListOf<Int>()
        val result = withTimeoutOrNull(1000) {
            for (item in channel) {
                received.add(item)
            }
            "completed"
        }
        assertNull("Consumer hangs forever — timed out waiting for more items", result)
        assertEquals("All sent items were received, but loop never terminates", listOf(1, 2, 3), received)
    }

    // BEST PRACTICE: produce{} auto-closes the channel when the block completes.
    @Test
    fun `BEST PRACTICE - produce builder auto-closes channel on completion`() = runTest {
        val channel = produce {
            for (i in 1..3) {
                send(i)
            }
            // produce{} auto-closes when block completes
        }
        val received = mutableListOf<Int>()
        for (item in channel) {
            received.add(item)
        }
        assertEquals("Consumer terminates cleanly after producer finishes", listOf(1, 2, 3), received)
    }

    @Test
    fun `channel for-loop terminates after close`() = runTest {
        val channel = Channel<Int>()
        launch {
            channel.send(1)
            channel.send(2)
            channel.send(3)
            channel.close()
        }
        val received = mutableListOf<Int>()
        for (item in channel) {
            received.add(item)
        }
        assertEquals("Should receive all items before close", listOf(1, 2, 3), received)
    }

    @Test
    fun `rendezvous channel send suspends without receiver`() = runTest {
        val channel = Channel<Int>() // rendezvous — capacity 0
        var sendCompleted = false
        val job = launch {
            channel.send(1)
            sendCompleted = true
        }
        yield()
        assertFalse("send should suspend on rendezvous channel without receiver", sendCompleted)
        channel.receive()
        job.join()
        assertTrue("send should complete after receive", sendCompleted)
    }

    @Test
    fun `conflated channel keeps only the latest value`() = runTest {
        val channel = Channel<Int>(Channel.CONFLATED)
        channel.send(1)
        channel.send(2)
        channel.send(3)
        val received = channel.receive()
        assertEquals("Conflated channel should keep only latest", 3, received)
    }

    // =====================================================================
    // Backpressure — buffer overflow strategies
    // =====================================================================

    // ANTIPATTERN: UNLIMITED buffer grows without bound — no signal to slow down.
    // With real data this leads to OutOfMemoryError.
    @Test
    fun `ANTIPATTERN - unlimited channel buffers everything with no backpressure`() = runTest {
        val channel = Channel<Int>(Channel.UNLIMITED)
        // Fast producer dumps all items — nothing pushes back
        for (i in 1..100) {
            channel.send(i)
        }
        channel.close()
        val received = mutableListOf<Int>()
        for (item in channel) {
            received.add(item)
        }
        assertEquals("All 100 items buffered in memory at once", 100, received.size)
        assertEquals("First item", 1, received.first())
        assertEquals("Last item", 100, received.last())
    }

    // BEST PRACTICE: DROP_OLDEST keeps only the latest value — bounded memory.
    // Ideal for UI state where only the most recent value matters.
    @Test
    fun `BEST PRACTICE - DROP_OLDEST channel keeps only the latest value`() = runTest {
        val channel = Channel<Int>(
            capacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        // Fast producer sends 3 items, buffer silently drops older ones
        channel.send(1)
        channel.send(2)
        channel.send(3)
        val received = channel.receive()
        assertEquals("DROP_OLDEST keeps the latest value", 3, received)
    }

    // DROP_LATEST keeps the first value — useful for rate-limiting / debounce.
    @Test
    fun `DROP_LATEST channel discards newest item when buffer full`() = runTest {
        val channel = Channel<Int>(
            capacity = 1,
            onBufferOverflow = BufferOverflow.DROP_LATEST
        )
        // Fast producer sends 3 items, buffer silently drops newer ones
        channel.send(1)
        channel.send(2)
        channel.send(3)
        val received = channel.receive()
        assertEquals("DROP_LATEST keeps the first value", 1, received)
    }

    // =====================================================================
    // async Exception Propagation
    // =====================================================================

    // ANTIPATTERN: async defers its exception until await() is called.
    // If you never await, the error is silently lost.
    @Test
    fun `ANTIPATTERN - async exception is silent until await`() = runTest {
        supervisorScope {
            val deferred = async {
                throw RuntimeException("boom")
            }
            // Exception happened, but no crash yet — it's stored in the Deferred
            delay(100)
            val result = runCatching { deferred.await() }
            assertTrue("Exception only surfaces when await() is called", result.isFailure)
            assertEquals("boom", result.exceptionOrNull()?.message)
        }
    }

    // BEST PRACTICE: try/catch around await() handles deferred exceptions cleanly.
    @Test
    fun `BEST PRACTICE - try-catch around await catches deferred exception`() = runTest {
        supervisorScope {
            val deferred = async {
                throw RuntimeException("network error")
            }
            val message = try {
                deferred.await()
            } catch (e: RuntimeException) {
                "Caught: ${e.message}"
            }
            assertEquals("Caught: network error", message)
        }
    }

    // =====================================================================
    // Flow Operators
    // =====================================================================

    @Test
    fun `map transforms each emission`() = runTest {
        val results = flow {
            emit(1)
            emit(2)
            emit(3)
        }.map { "Item $it" }
            .toList()
        assertEquals(listOf("Item 1", "Item 2", "Item 3"), results)
    }

    @Test
    fun `filter drops emissions not matching predicate`() = runTest {
        val results = flow {
            emit(1)
            emit(2)
            emit(3)
            emit(4)
        }.filter { it % 2 == 0 }
            .toList()
        assertEquals(listOf(2, 4), results)
    }

    @Test
    fun `combine merges latest values from two flows`() = runTest {
        val flow1 = flow {
            emit("A")
            delay(100)
            emit("B")
        }
        val flow2 = flow {
            emit(1)
            delay(150)
            emit(2)
        }
        val results = combine(flow1, flow2) { letter, number -> "$letter$number" }
            .toList()
        // t=0: A+1 → "A1", t=100: B+1 → "B1", t=150: B+2 → "B2"
        assertEquals(listOf("A1", "B1", "B2"), results)
    }

    @Test
    fun `flatMapLatest cancels previous inner flow on new emission`() = runTest {
        val results = flow {
            emit("A")
            delay(100)
            emit("B")
        }.flatMapLatest { value ->
            flow {
                emit("${value}1")
                delay(200)
                emit("${value}2") // "A2" should be cancelled when "B" arrives
            }
        }.toList()
        assertTrue("A1 should be collected", "A1" in results)
        assertTrue("B1 should be collected", "B1" in results)
        assertTrue("B2 should be collected (final outer emission)", "B2" in results)
        assertFalse("A2 should be cancelled by flatMapLatest", "A2" in results)
    }

    @Test
    fun `conflate drops intermediate values when collector is slow`() = runTest {
        val results = mutableListOf<Int>()
        flow {
            emit(1)
            delay(1) // small delay so emissions are not synchronous
            emit(2)
            delay(1)
            emit(3)
        }.conflate()
            .collect { value ->
                delay(100) // slow collector
                results.add(value)
            }
        assertTrue("First value is collected", 1 in results)
        assertTrue("Last value is collected", 3 in results)
        assertTrue("Conflate drops intermediate values when collector is slow", results.size < 3)
    }

    // =====================================================================
    // flowOn & Emission Context
    // =====================================================================

    @Test
    fun `flowOn switches upstream dispatcher`() = runTest {
        val emissionThreadNames = mutableListOf<String>()
        flow {
            emissionThreadNames.add(Thread.currentThread().name)
            emit(1)
        }
            .flowOn(Dispatchers.Default)
            .collect { }
        assertTrue(
            "Emission should run on Default dispatcher, was: ${emissionThreadNames.first()}",
            emissionThreadNames.first().contains("DefaultDispatcher")
        )
    }

    // ANTIPATTERN: emitting from a different context inside flow {} violates the flow invariant.
    @Test
    fun `ANTIPATTERN - emitting from wrong context throws`() = runTest {
        val result = runCatching {
            flow {
                withContext(Dispatchers.Default) {
                    emit(1) // Throws IllegalStateException — flow context changed
                }
            }.collect { }
        }
        assertTrue("Emitting from different context should throw", result.isFailure)
        assertTrue(
            "Should be IllegalStateException",
            result.exceptionOrNull() is IllegalStateException
        )
    }

    // =====================================================================
    // SharedFlow
    // =====================================================================

    @Test
    fun `SharedFlow with replay 0 loses events emitted before subscriber`() = runTest {
        val sharedFlow = MutableSharedFlow<Int>(replay = 0, extraBufferCapacity = 10)
        // Emit before any subscriber
        sharedFlow.tryEmit(1)
        sharedFlow.tryEmit(2)

        // Now subscribe
        val collected = mutableListOf<Int>()
        val job = launch {
            sharedFlow.collect { collected.add(it) }
        }
        yield() // let collector start

        // Emit after subscriber
        sharedFlow.emit(3)
        yield()

        job.cancel()
        assertFalse("Event 1 before subscriber is lost with replay=0", 1 in collected)
        assertFalse("Event 2 before subscriber is lost with replay=0", 2 in collected)
        assertTrue("Event after subscriber is received", 3 in collected)
    }

    @Test
    fun `SharedFlow with replay 1 delivers last event to new subscriber`() = runTest {
        val sharedFlow = MutableSharedFlow<Int>(replay = 1)
        sharedFlow.emit(1)
        sharedFlow.emit(2) // This is the last one before subscriber

        val collected = mutableListOf<Int>()
        val job = launch {
            sharedFlow.collect { collected.add(it) }
        }
        yield()
        job.cancel()
        assertEquals("New subscriber gets the last replayed value", listOf(2), collected)
    }

    @Test
    fun `SharedFlow with extraBufferCapacity buffers events`() = runTest {
        val sharedFlow = MutableSharedFlow<Int>(replay = 0, extraBufferCapacity = 5)
        val collected = mutableListOf<Int>()

        // Start a slow subscriber
        val job = launch {
            sharedFlow.collect {
                delay(100) // slow processing
                collected.add(it)
            }
        }
        yield()

        // Fast emitter — extra buffer prevents suspension
        for (i in 1..3) {
            val emitted = sharedFlow.tryEmit(i)
            assertTrue("tryEmit should succeed with buffer capacity", emitted)
        }

        // Wait for collector to process
        delay(500)
        job.cancel()

        assertEquals("All buffered events should be delivered", listOf(1, 2, 3), collected)
    }

    // =====================================================================
    // Retry Patterns
    // =====================================================================

    @Test
    fun `retry succeeds after transient failures`() = runTest {
        var attempts = 0
        val result = flow {
            attempts++
            if (attempts <= 2) throw IOException("transient failure")
            emit("success")
        }.retry(2)
            .toList()
        assertEquals(listOf("success"), result)
        assertEquals("Should take 3 attempts (2 failures + 1 success)", 3, attempts)
    }

    @Test
    fun `retryWhen filters by exception type`() = runTest {
        var attempts = 0
        val result = runCatching {
            flow<String> {
                attempts++
                if (attempts == 1) throw IOException("retryable")
                if (attempts == 2) throw IllegalStateException("not retryable")
                emit("should not reach")
            }.retryWhen { cause, _ ->
                cause is IOException // only retry IOExceptions
            }.toList()
        }
        assertTrue("Non-IOException should not be retried", result.isFailure)
        assertTrue(
            "Should rethrow IllegalStateException",
            result.exceptionOrNull() is IllegalStateException
        )
        assertEquals("Should attempt twice (IOException retried, ISE not)", 2, attempts)
    }

    // =====================================================================
    // Mutual Exclusion
    // =====================================================================

    // ANTIPATTERN: interleaved read-modify-write loses updates without synchronization.
    @Test
    fun `ANTIPATTERN - shared mutable state without synchronization loses updates`() = runTest {
        var counter = 0
        coroutineScope {
            launch {
                val snapshot = counter // reads 0
                yield() // other coroutine runs here
                counter = snapshot + 1 // writes 1
            }
            launch {
                val snapshot = counter // also reads 0
                yield()
                counter = snapshot + 1 // also writes 1
            }
        }
        // Both read 0, both write 1 — one update is lost.
        assertEquals("One update lost due to read-modify-write interleaving", 1, counter)
    }

    // BEST PRACTICE: Mutex serializes access so no updates are lost.
    @Test
    fun `BEST PRACTICE - Mutex protects shared mutable state`() = runTest {
        var counter = 0
        val mutex = Mutex()
        coroutineScope {
            launch {
                mutex.withLock {
                    val snapshot = counter
                    yield() // yield inside lock — other coroutine waits
                    counter = snapshot + 1
                }
            }
            launch {
                mutex.withLock {
                    val snapshot = counter
                    yield()
                    counter = snapshot + 1
                }
            }
        }
        assertEquals("Mutex prevents lost updates", 2, counter)
    }

    // =====================================================================
    // callbackFlow
    // =====================================================================

    @Test
    fun `callbackFlow converts callback API to Flow`() = runTest {
        var callback: ((Int) -> Unit)? = null

        val flow = callbackFlow<Int> {
            callback = { value -> trySend(value) }
            awaitClose { callback = null }
        }

        val collected = mutableListOf<Int>()
        val job = launch {
            flow.collect { collected.add(it) }
        }

        // Need multiple yields: first starts the collector, second starts the producer
        yield()
        yield()

        callback?.invoke(1)
        yield()
        callback?.invoke(2)
        yield()

        job.cancel()
        assertEquals(listOf(1, 2), collected)
    }

    @Test
    fun `awaitClose runs cleanup when flow collector cancels`() = runTest {
        var cleanedUp = false

        val flow = callbackFlow {
            trySend(1)
            awaitClose { cleanedUp = true }
        }

        val job = launch {
            flow.collect { }
        }
        yield()

        assertFalse("Cleanup should not run while collecting", cleanedUp)
        job.cancel()
        job.join()
        assertTrue("awaitClose cleanup should run after cancellation", cleanedUp)
    }
}
