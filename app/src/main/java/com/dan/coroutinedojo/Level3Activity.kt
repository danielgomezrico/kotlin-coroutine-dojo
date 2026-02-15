package com.dan.coroutinedojo

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.dan.coroutinedojo.ui.components.LessonScreen
import com.dan.coroutinedojo.ui.theme.CoroutineDojoTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class Level3Activity : ComponentActivity() {

    private val tickFlow = MutableStateFlow(0)

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CoroutineDojoTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text("Level 3: StateFlow & SharedFlow") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        var statusText by remember { mutableStateOf("Ready") }
                        val lifecycleAwareValue by tickFlow.collectAsStateWithLifecycle()

                        LessonScreen(
                            objective = "Collect Flows with lifecycle awareness to avoid wasting resources when the app is backgrounded.",
                            antipatternTitle = "Non-Lifecycle-Aware Collection",
                            antipatternDescription = "lifecycleScope.launch { flow.collect {} } keeps collecting even when the app is in the background, wasting CPU and battery.",
                            onAntipatternClick = {
                                statusText = "Antipattern: Starting counter...\n" +
                                    "Collection continues in background!\n" +
                                    "Check Logcat tag 'Level3' — ticks keep logging even when backgrounded."
                                lifecycleScope.launch {
                                    var tick = 0
                                    while (true) {
                                        tick++
                                        Log.d("Level3", "Antipattern collecting tick: $tick")
                                        statusText = "Antipattern tick: $tick\n(collecting even in background)"
                                        delay(1000)
                                    }
                                }
                            },
                            bestPracticeTitle = "collectAsStateWithLifecycle",
                            bestPracticeDescription = "collectAsStateWithLifecycle() stops collection when the lifecycle drops below STARTED and restarts when it resumes.",
                            onBestPracticeClick = {
                                statusText = "Best Practice: Starting counter...\n" +
                                    "Collection pauses when backgrounded, resumes in foreground.\n" +
                                    "Watch the counter — it pauses when you leave the app."
                                lifecycleScope.launch {
                                    var tick = 0
                                    while (true) {
                                        tick++
                                        tickFlow.value = tick
                                        delay(1000)
                                    }
                                }
                            },
                            explanation = "The value from collectAsStateWithLifecycle: $lifecycleAwareValue\n\n" +
                                "collectAsStateWithLifecycle (from lifecycle-runtime-compose) automatically handles lifecycle. " +
                                "It collects when the lifecycle is at least STARTED and cancels when it drops below.\n\n" +
                                "KEY PITFALL — StateFlow equality-based conflation:\n" +
                                "StateFlow compares new values with equals(). If you emit the same data class instance twice, " +
                                "the second emission is silently dropped. Collectors never see it. " +
                                "Workaround: add a unique ID field, or use SharedFlow(replay=1) instead.\n\n" +
                                "KEY PITFALL — SharedFlow(replay=0) loses events:\n" +
                                "Events emitted before a subscriber starts collecting are lost forever. " +
                                "For one-shot events (navigation, snackbar), a Channel is a better fit since it buffers.\n\n" +
                                "PITFALL — Hot becomes cold after operators:\n" +
                                ".map{} on a StateFlow returns a cold Flow — each collector runs its own operator chain independently. " +
                                "To re-share the result, use stateIn() or shareIn() with a scope.",
                            statusText = statusText
                        )
                    }
                }
            }
        }
    }
}
