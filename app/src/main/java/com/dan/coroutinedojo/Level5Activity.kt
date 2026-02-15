package com.dan.coroutinedojo

import android.os.Bundle
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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Level5Activity : ComponentActivity() {

    private suspend fun fetchProfile(): String {
        delay(2000)
        return "User Profile"
    }

    private suspend fun fetchSettings(): String {
        delay(2000)
        return "User Settings"
    }

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
                            title = { Text("Level 5: Parallel Decomposition") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        val scope = rememberCoroutineScope()
                        var statusText by remember { mutableStateOf("Ready") }

                        LessonScreen(
                            objective = "Run independent operations concurrently instead of sequentially to improve performance.",
                            antipatternTitle = "Sequential Suspend Calls",
                            antipatternDescription = "Calling two independent suspend functions one after another " +
                                "doubles the total wait time.",
                            onAntipatternClick = {
                                statusText = "Sequential: Fetching..."
                                scope.launch {
                                    val start = System.currentTimeMillis()
                                    val profile = fetchProfile()   // ~2s
                                    val settings = fetchSettings() // ~2s
                                    val elapsed = System.currentTimeMillis() - start
                                    statusText = "Sequential completed in ${elapsed}ms\n" +
                                        "Profile: $profile\n" +
                                        "Settings: $settings\n\n" +
                                        "~4 seconds — each call waited for the previous one."
                                }
                            },
                            bestPracticeTitle = "Concurrent with async/await",
                            bestPracticeDescription = "Wrapping independent calls in async {} runs them " +
                                "concurrently within a coroutineScope.",
                            onBestPracticeClick = {
                                statusText = "Concurrent: Fetching..."
                                scope.launch {
                                    val start = System.currentTimeMillis()
                                    val (profile, settings) = coroutineScope {
                                        val profileDeferred = async { fetchProfile() }
                                        val settingsDeferred = async { fetchSettings() }
                                        Pair(profileDeferred.await(), settingsDeferred.await())
                                    }
                                    val elapsed = System.currentTimeMillis() - start
                                    statusText = "Concurrent completed in ${elapsed}ms\n" +
                                        "Profile: $profile\n" +
                                        "Settings: $settings\n\n" +
                                        "~2 seconds — both calls ran in parallel."
                                }
                            },
                            explanation = "async {} returns a Deferred<T> and starts the coroutine " +
                                "immediately within its scope.\n\n" +
                                "coroutineScope {} ensures structured concurrency — if one async " +
                                "fails, the other is cancelled automatically. " +
                                "This prevents orphaned work.\n\n" +
                                "awaitAll() is a shorthand for waiting on multiple deferreds:\n" +
                                "val (a, b) = awaitAll(deferred1, deferred2)\n\n" +
                                "WHEN NOT TO PARALLELIZE:\n" +
                                "If call B needs the result of call A, they must be sequential. " +
                                "async is only for independent operations.\n\n" +
                                "MEASURE with measureTimeMillis {} or System.currentTimeMillis() " +
                                "to verify the speedup.",
                            statusText = statusText
                        )
                    }
                }
            }
        }
    }
}
