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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dan.coroutinedojo.ui.components.LessonScreen
import com.dan.coroutinedojo.ui.theme.CoroutineDojoTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class Level6Activity : ComponentActivity() {
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
                            title = { Text("Level 6: ViewModel & Lifecycle") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        val vm: Level6ViewModel = viewModel()
                        val tickCount by vm.tickCount.collectAsStateWithLifecycle()
                        val vmStatus by vm.status.collectAsStateWithLifecycle()

                        var antipatternTick by remember { mutableStateOf(0) }
                        var statusText by remember { mutableStateOf("Ready") }

                        LessonScreen(
                            objective = "Use ViewModel + lifecycle-aware collection for production-grade flow handling.",
                            antipatternTitle = "Activity-Scoped Collection",
                            antipatternDescription = "Collecting a flow in lifecycleScope without a ViewModel " +
                                "means data is lost on configuration changes (rotation). " +
                                "The counter resets each time.",
                            onAntipatternClick = {
                                antipatternTick = 0
                                statusText = "Antipattern: Counter started in lifecycleScope.\n" +
                                    "Rotate the device — the counter resets!"
                                lifecycleScope.launch {
                                    val localFlow = MutableStateFlow(0)
                                    launch {
                                        while (true) {
                                            delay(1000)
                                            localFlow.value++
                                            antipatternTick = localFlow.value
                                            statusText = "Antipattern tick: $antipatternTick\n" +
                                                "(will reset on rotation)"
                                            Log.d("Level6-Anti", "Antipattern tick: ${localFlow.value}")
                                        }
                                    }
                                }
                            },
                            bestPracticeTitle = "ViewModel + collectAsStateWithLifecycle",
                            bestPracticeDescription = "ViewModel's viewModelScope survives rotation. " +
                                "collectAsStateWithLifecycle pauses collection when backgrounded.",
                            onBestPracticeClick = {
                                vm.startTicker()
                                vm.triggerWork()
                                statusText = "Best Practice: ViewModel ticker started.\n" +
                                    "Rotate the device — the counter survives!"
                            },
                            explanation = "ViewModel tick: $tickCount | Status: $vmStatus\n\n" +
                                "viewModelScope survives configuration changes (rotation, locale change). " +
                                "Data loading in viewModelScope doesn't restart.\n\n" +
                                "collectAsStateWithLifecycle (Compose) pauses collection when the lifecycle " +
                                "drops below STARTED (app backgrounded), saving battery.\n\n" +
                                "The View-system equivalent is repeatOnLifecycle(Lifecycle.State.STARTED) " +
                                "— it stops and restarts collection based on lifecycle state.\n\n" +
                                "ARCHITECTURE PATTERN:\n" +
                                "ViewModel exposes StateFlow -> Activity/Compose collects with lifecycle awareness.\n\n" +
                                "SCOPE RECAP:\n" +
                                "- viewModelScope — data loading, business logic (survives rotation)\n" +
                                "- lifecycleScope — UI-only work (dies with Activity)\n" +
                                "- rememberCoroutineScope — Compose interaction effects (dies with composition)",
                            statusText = statusText
                        )
                    }
                }
            }
        }
    }
}
