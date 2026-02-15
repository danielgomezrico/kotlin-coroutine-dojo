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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.dan.coroutinedojo.ui.components.LessonScreen
import com.dan.coroutinedojo.ui.theme.CoroutineDojoTheme
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class Level4Activity : ComponentActivity() {
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
                            title = { Text("Level 4: Exception Handling") },
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

                        // Scope with CoroutineExceptionHandler for the antipattern demo
                        val handler = CoroutineExceptionHandler { _, throwable ->
                            statusText = "Antipattern: CoroutineExceptionHandler caught:\n" +
                                "\"${throwable.message}\"\n\n" +
                                "The try/catch around launch() did NOT catch this.\n" +
                                "The exception propagated to the scope's handler instead."
                            Log.e("Level4", "Uncaught in coroutine", throwable)
                        }
                        val antipatternScope = remember {
                            CoroutineScope(SupervisorJob() + handler)
                        }
                        val scope = rememberCoroutineScope()

                        LessonScreen(
                            objective = "Handle exceptions correctly in coroutines without crashing or silently losing errors.",
                            antipatternTitle = "try/catch Around launch() Doesn't Work",
                            antipatternDescription = "Wrapping launch {} in try/catch does nothing — launch starts " +
                                "asynchronously, so the exception propagates to the scope, not the caller.",
                            onAntipatternClick = {
                                statusText = "Antipattern: Trying to catch exception outside launch..."
                                try {
                                    antipatternScope.launch {
                                        throw RuntimeException("Simulated network error")
                                    }
                                    // This line runs immediately — launch is fire-and-forget.
                                    // The try/catch will NOT catch the coroutine's exception.
                                } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                                    // This never executes for the coroutine's exception
                                    statusText = "This never shows — try/catch can't catch launch exceptions"
                                }
                            },
                            bestPracticeTitle = "Handle Inside the Coroutine Body",
                            bestPracticeDescription = "Put try/catch inside the launch block to handle exceptions " +
                                "at the coroutine boundary.",
                            onBestPracticeClick = {
                                statusText = "Best Practice: Handling exception inside launch..."
                                scope.launch {
                                    try {
                                        // Simulate failing work
                                        throw RuntimeException("Simulated network error")
                                    } catch (e: RuntimeException) {
                                        statusText = "Best Practice: Caught inside launch:\n" +
                                            "\"${e.message}\"\n\n" +
                                            "Exception handled cleanly — no crash, no silent loss."
                                    }
                                }
                            },
                            explanation = "LAUNCH propagates exceptions to the parent scope. " +
                                "If unhandled, this crashes the app.\n\n" +
                                "ASYNC defers exceptions until await() is called. " +
                                "If you never await, the error is silently lost — a common trap.\n\n" +
                                "CoroutineExceptionHandler is a LAST RESORT — install it on a scope " +
                                "to catch otherwise-unhandled exceptions. " +
                                "It's not a replacement for proper try/catch.\n\n" +
                                "supervisorScope isolates children so one failure doesn't cancel siblings. " +
                                "Without it, one child's exception cancels all siblings in a coroutineScope.\n\n" +
                                "RULE: Always handle exceptions at the coroutine boundary " +
                                "(inside the launch/async block), not around the builder call.",
                            statusText = statusText
                        )
                    }
                }
            }
        }
    }
}
