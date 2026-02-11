package com.dan.coroutinedojo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.dan.coroutinedojo.ui.components.LessonScreen
import com.dan.coroutinedojo.ui.theme.CoroutineDojoTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class Level5Activity : ComponentActivity() {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CoroutineDojoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        val scope = rememberCoroutineScope()
                        var statusText by remember { mutableStateOf("Ready") }

                        LessonScreen(
                            title = "Level 5: Channels & Backpressure",
                            objective = "Understand Channel lifecycle — always close channels to avoid hanging consumers.",
                            antipatternTitle = "Unclosed Channel (Consumer Hangs)",
                            antipatternDescription = "Producer sends items into a Channel but never calls close(). " +
                                "The consumer's for-loop waits forever for more items.",
                            onAntipatternClick = {
                                statusText = "Starting unclosed channel demo...\n"
                                scope.launch {
                                    val channel = Channel<Int>()
                                    launch {
                                        for (i in 1..3) {
                                            channel.send(i)
                                            statusText += "Sent: $i\n"
                                            delay(300)
                                        }
                                        statusText += "Producer done (but forgot to close!)\n"
                                        // Bug: no channel.close()
                                    }
                                    launch {
                                        val result = withTimeoutOrNull(3000) {
                                            for (item in channel) {
                                                statusText += "Received: $item\n"
                                            }
                                            "Consumer finished"
                                        }
                                        if (result == null) {
                                            statusText += "Consumer TIMED OUT — hanging forever waiting for more items!\n" +
                                                "(In real code there's no timeout — it hangs indefinitely)"
                                        }
                                    }
                                }
                            },
                            bestPracticeTitle = "produce{} Builder (Auto-Close)",
                            bestPracticeDescription = "The produce{} coroutine builder auto-closes the channel when the block completes. " +
                                "Consumer terminates cleanly.",
                            onBestPracticeClick = {
                                statusText = "Starting produce{} demo...\n"
                                scope.launch {
                                    val channel = produce(capacity = Channel.BUFFERED) {
                                        for (i in 1..3) {
                                            send(i)
                                            statusText += "Sent: $i\n"
                                            delay(300)
                                        }
                                        statusText += "Producer done (channel auto-closes)\n"
                                    }
                                    for (item in channel) {
                                        statusText += "Received: $item\n"
                                    }
                                    statusText += "Consumer finished cleanly!"
                                }
                            },
                            explanation = "Channels are hot stream primitives for coroutine-to-coroutine communication. " +
                                "Unlike Flow, they have a buffer and support multiple producers/consumers.\n\n" +
                                "KEY PITFALL — Forgetting to close:\n" +
                                "A for-loop on a channel suspends forever waiting for more items unless the channel is closed. " +
                                "The produce{} builder solves this by auto-closing on completion or cancellation.\n\n" +
                                "KEY PITFALL — send() vs trySend():\n" +
                                "send() is a suspend function — it suspends when the buffer is full. " +
                                "You can't call it from non-suspending contexts like click handlers. " +
                                "trySend() returns immediately with success/failure. " +
                                "Combine trySend() with Channel.CONFLATED or Channel.DROP_OLDEST for UI events.\n\n" +
                                "PITFALL — Rendezvous deadlocks:\n" +
                                "Two coroutines each doing send-then-receive on opposite rendezvous channels deadlock — " +
                                "both block on send with nobody receiving. Fix: add a buffer.\n\n" +
                                "CHANNEL vs FLOW — when to choose:\n" +
                                "Prefer Flow for most stream use cases — it's simpler and supports operators. " +
                                "Use Channel only for fan-out (multiple consumers), fan-in (multiple producers), " +
                                "or true producer-consumer queues between independent coroutines.",
                            statusText = statusText
                        )
                    }
                }
            }
        }
    }
}
