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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Level1Activity : ComponentActivity() {
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
                            title = "Level 1: Dispatchers",
                            objective = "Perform a long-running operation without blocking the UI thread.",
                            antipatternTitle = "Blocking the Main Thread",
                            antipatternDescription = "Sleeping the thread directly on the Main thread freezes the UI. The app becomes unresponsive.",
                            onAntipatternClick = {
                                statusText = "Antipattern Started... (UI will freeze for 3s)"
                                // Using a Handler to let the UI update before freezing
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    try {
                                        Thread.sleep(3000) // The Bad Part
                                        statusText = "Antipattern Finished (UI Thawed)"
                                    } catch (e: InterruptedException) {
                                        statusText = "Interrupted"
                                    }
                                }, 100)
                            },
                            bestPracticeTitle = "Offloading to Dispatchers.IO",
                            bestPracticeDescription = "Using withContext(Dispatchers.IO) helps offload blocking operations to a background thread.",
                            onBestPracticeClick = {
                                statusText = "Best Practice Started..."
                                scope.launch {
                                    // Simulate work on IO
                                    val result = withContext(Dispatchers.IO) {
                                        // Heavy computation or blocking IO
                                        Thread.sleep(3000)
                                        "Result from IO"
                                    }
                                    statusText = "Best Practice Finished: $result"
                                }
                            },
                            explanation = "The Main thread is responsible for drawing the UI. If you block it (e.g. Thread.sleep), no frames can be drawn, and the app appears frozen (ANR).\n\n" +
                                    "Coroutines allow us to switch dispatchers easily. Dispatchers.IO is designed for offloading blocking I/O operations (like network or disk calls), while Dispatchers.Default is for CPU-intensive tasks.\n\n" +
                                    "Properly scoping your work ensures a responsive UI.",
                            statusText = statusText
                        )
                    }
                }
            }
        }
    }
}
