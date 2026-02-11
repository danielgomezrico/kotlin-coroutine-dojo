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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Level3Activity : ComponentActivity() {
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
                                title = "Level 3: Structured Concurrency",
                                objective =
                                        "Maintain parent-child relationships so that cancellation propagates correctly.",
                                antipatternTitle = "Unstructured (GlobalScope)",
                                antipatternDescription =
                                        "Children launched in GlobalScope are not tied to the parent. They survive parent cancellation.",
                                onAntipatternClick = {
                                    statusText = "Starting Orphan Demo...\n"
                                    val parentJob =
                                            scope.launch {
                                                statusText += "Parent running...\n"
                                                @OptIn(DelicateCoroutinesApi::class)
                                                GlobalScope.launch {
                                                    try {
                                                        delay(2000)
                                                        statusText +=
                                                                "Orphan: I finished despite parent cancel!\n"
                                                    } catch (e: Exception) {
                                                        statusText +=
                                                                "Orphan: I was cancelled? (Unexpected)\n"
                                                    }
                                                }
                                            }

                                    scope.launch {
                                        delay(500)
                                        statusText += "Cancelling Parent...\n"
                                        parentJob.cancel()
                                    }
                                },
                                bestPracticeTitle = "Structured (coroutineScope)",
                                bestPracticeDescription =
                                        "Nested 'launch' creates a hierarchy. Cancelling the parent automatically cancels all children.",
                                onBestPracticeClick = {
                                    statusText = "Starting Structured Demo...\n"
                                    val parentJob =
                                            scope.launch {
                                                statusText += "Parent running...\n"
                                                launch {
                                                    try {
                                                        delay(2000)
                                                        statusText +=
                                                                "Child: I finished! (Unexpected)\n"
                                                    } catch (e: CancellationException) {
                                                        statusText +=
                                                                "Child: I was cancelled immediately!\n"
                                                    }
                                                }
                                            }

                                    scope.launch {
                                        delay(500)
                                        statusText += "Cancelling Parent...\n"
                                        parentJob.cancel()
                                    }
                                },
                                explanation =
                                        "Structured Concurrency ensures no child is left behind. \n\n" +
                                                "When a scope is cancelled (or fails), all its children are cancelled recursively.\n" +
                                                "This prevents memory leaks and 'zombie' tasks.",
                                statusText = statusText
                        )
                    }
                }
            }
        }
    }
}
