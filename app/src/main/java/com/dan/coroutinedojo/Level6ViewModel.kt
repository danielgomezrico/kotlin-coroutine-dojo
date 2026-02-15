package com.dan.coroutinedojo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class Level6ViewModel : ViewModel() {
    private val _tickCount = MutableStateFlow(0)
    val tickCount: StateFlow<Int> = _tickCount

    private val _status = MutableStateFlow("ViewModel ready")
    val status: StateFlow<String> = _status

    fun startTicker() {
        _status.value = "Ticker running (survives rotation)"
        viewModelScope.launch {
            while (true) {
                delay(1000)
                _tickCount.value++
            }
        }
    }

    fun triggerWork() {
        _status.value = "Working..."
        viewModelScope.launch {
            delay(2000)
            _status.value = "Work complete (survived rotation)"
        }
    }
}
