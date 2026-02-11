package com.dan.coroutinedojo

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineDojoTest {

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

    @Test
    fun `coroutineScope cancels sibling when one child fails`() = runTest {
        var siblingCancelled = false
        val result = runCatching {
            coroutineScope {
                launch {
                    try {
                        delay(Long.MAX_VALUE)
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
        assertTrue("coroutineScope should throw", result.isFailure)
        assertTrue("Sibling should be cancelled", siblingCancelled)
    }

    @Test
    fun `supervisorScope lets sibling complete when one child fails`() = runTest {
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
        assertTrue("Sibling should complete despite other child failing", siblingCompleted)
    }

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

    @Test
    fun `conflated channel keeps only the latest value`() = runTest {
        val channel = Channel<Int>(Channel.CONFLATED)
        channel.send(1)
        channel.send(2)
        channel.send(3)
        val received = channel.receive()
        assertEquals("Conflated channel should keep only latest", 3, received)
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
}
