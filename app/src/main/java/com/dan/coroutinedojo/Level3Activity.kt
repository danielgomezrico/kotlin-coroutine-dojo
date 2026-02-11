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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

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
                            objective = "Handle exceptions correctly in concurrent coroutines using async/await and supervisorScope.",
                            antipatternTitle = "async Exception Collateral Damage",
                            antipatternDescription = "Two async tasks in coroutineScope — when one fails, the other is cancelled as collateral damage. " +
                                "Try-catch around async{} (not await) does nothing.",
                            onAntipatternClick = {
                                statusText = "Starting coroutineScope demo...\n"
                                scope.launch {
                                    try {
                                        coroutineScope {
                                            val task1 = async {
                                                delay(500)
                                                throw RuntimeException("Task 1 failed!")
                                            }
                                            val task2 = async {
                                                delay(2000)
                                                "Task 2 completed"
                                            }
                                            statusText += "Both tasks launched...\n"
                                            try {
                                                val result2 = task2.await()
                                                statusText += "Task 2: $result2\n"
                                            } catch (e: RuntimeException) {
                                                statusText += "Task 2 await threw: ${e.message}\n"
                                            }
                                            try {
                                                task1.await()
                                            } catch (e: RuntimeException) {
                                                statusText += "Task 1 await caught: ${e.message}\n"
                                            }
                                        }
                                    } catch (e: RuntimeException) {
                                        statusText += "coroutineScope failed: ${e.message}\n"
                                        statusText += "Task 2 was cancelled as collateral damage!"
                                    }
                                }
                            },
                            bestPracticeTitle = "supervisorScope Isolation",
                            bestPracticeDescription = "supervisorScope prevents one child's failure from cancelling siblings. " +
                                "Try-catch around await() handles individual failures.",
                            onBestPracticeClick = {
                                statusText = "Starting supervisorScope demo...\n"
                                scope.launch {
                                    supervisorScope {
                                        val task1 = async {
                                            delay(500)
                                            throw RuntimeException("Task 1 failed!")
                                        }
                                        val task2 = async {
                                            delay(1500)
                                            "Task 2 completed successfully"
                                        }
                                        statusText += "Both tasks launched...\n"

                                        val result1 = try {
                                            task1.await()
                                        } catch (e: RuntimeException) {
                                            statusText += "Task 1 failed: ${e.message}\n"
                                            "Task 1 handled"
                                        }

                                        val result2 = try {
                                            task2.await()
                                        } catch (e: RuntimeException) {
                                            "Task 2 failed: ${e.message}"
                                        }
                                        statusText += "Task 2 result: $result2\n"
                                        statusText += "Both tasks handled independently!"
                                    }
                                }
                            },
                            explanation = "In coroutineScope, a child failure cancels ALL siblings (structured concurrency). " +
                                "This is correct for dependent tasks but wrong when tasks are independent.\n\n" +
                                "supervisorScope lets each child fail independently. " +
                                "Catch exceptions at the await() call — NOT around async{}.\n\n" +
                                "CLASSIC MISUSE — SupervisorJob() as builder argument:\n" +
                                "launch(SupervisorJob()) { ... } does NOT supervise the inner children. " +
                                "The SupervisorJob becomes the parent of the launch, not of children inside it. " +
                                "Use supervisorScope{} instead.\n\n" +
                                "PITFALL — Swallowing CancellationException:\n" +
                                "catch(e: Exception) or runCatching {} catches CancellationException too, " +
                                "creating zombie coroutines that refuse to cancel. " +
                                "Always rethrow CancellationException, or use catch(e: Exception) with an ensureActive() call.\n\n" +
                                "PITFALL — Cooperative cancellation:\n" +
                                "cancel() only works at suspension points. CPU-bound loops without suspension " +
                                "ignore cancellation. Use ensureActive() or check isActive in tight loops.\n\n" +
                                "NOTE: CoroutineExceptionHandler only works on root coroutines. " +
                                "Installing it on child coroutines is silently ignored.",
                            statusText = statusText
                        )
                    }
                }
            }
        }
    }
}
