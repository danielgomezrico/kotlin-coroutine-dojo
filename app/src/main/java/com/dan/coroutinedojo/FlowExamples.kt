package com.dan.coroutinedojo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FlowExamples(private val scope: CoroutineScope) {

    private val _counter = MutableStateFlow(0)
    val counter: StateFlow<Int> = _counter.asStateFlow()

    private val _events = MutableSharedFlow<String>(replay = 0)
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun startCounter() {
        scope.launch {
            var i = 0
            while (true) {
                _counter.value = i
                i++
                delay(1000)
            }
        }
    }

    suspend fun emitDuplicateValues(): List<Int> {
        val stateFlow = MutableStateFlow(0)
        val collected = mutableListOf<Int>()
        val job = scope.launch {
            stateFlow.collect { collected.add(it) }
        }
        delay(50)
        stateFlow.value = 1
        delay(50)
        stateFlow.value = 1 // duplicate — will be dropped by StateFlow
        delay(50)
        stateFlow.value = 2
        delay(50)
        job.cancel()
        return collected
    }

    suspend fun emitBeforeSubscriber(): String {
        val sharedFlow = MutableSharedFlow<String>(replay = 0)
        sharedFlow.emit("event-before-subscriber")
        var received = "nothing"
        val job = scope.launch {
            sharedFlow.collect { received = it }
        }
        delay(50)
        sharedFlow.emit("event-after-subscriber")
        delay(50)
        job.cancel()
        return received
    }
}
