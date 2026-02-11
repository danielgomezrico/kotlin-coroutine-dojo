package com.dan.coroutinedojo

import android.os.Bundle
import android.util.Log
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.dan.coroutinedojo.ui.components.LessonScreen
import com.dan.coroutinedojo.ui.theme.CoroutineDojoTheme
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Level2Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CoroutineDojoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        var statusText by remember {
                            mutableStateOf("Ready. Check Logcat with tag 'Level2'.")
                        }

                        LessonScreen(
                                title = "Level 2: Lifecycle & Scopes",
                                objective =
                                        "Ensure coroutines are cancelled when the UI is destroyed to avoid leaks.",
                                antipatternTitle = "GlobalScope / Forgotten Cancellation",
                                antipatternDescription =
                                        "Launching into GlobalScope means the coroutine runs until the app process dies, even if this Activity is closed.",
                                onAntipatternClick = {
                                    statusText =
                                            "GlobalScope launched. Check Logcat 'Level2-Global'.\nClose Activity -> It keeps running!"
                                    @OptIn(DelicateCoroutinesApi::class)
                                    GlobalScope.launch {
                                        var i = 0
                                        while (true) {
                                            Log.d("Level2-Global", "GlobalScope running... $i")
                                            i++
                                            delay(1000)
                                        }
                                    }
                                },
                                bestPracticeTitle = "lifecycleScope",
                                bestPracticeDescription =
                                        "lifecycleScope is tied to the Activity/Fragment lifecycle. It cancels automatically when destroyed.",
                                onBestPracticeClick = {
                                    statusText =
                                            "lifecycleScope launched. Check Logcat 'Level2-Lifecycle'.\nClose Activity -> It stops."
                                    lifecycleScope.launch {
                                        var i = 0
                                        while (true) {
                                            Log.d(
                                                    "Level2-Lifecycle",
                                                    "lifecycleScope running... $i"
                                            )
                                            i++
                                            delay(1000)
                                        }
                                    }
                                },
                                explanation =
                                        "Coroutines must be scoped. \n\n" +
                                                "GlobalScope is rarely needed. It has no bounds.\n\n" +
                                                "lifecycleScope (available in Activity/Fragment) ensures that when the UI is destroyed, the background work is cancelled, freeing up resources and preventing crashes.",
                                statusText = statusText
                        )
                    }
                }
            }
        }
    }
}
