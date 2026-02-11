package com.dan.coroutinedojo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.launch

class ChannelExamples(private val scope: CoroutineScope) {

    fun produceWithoutClose(count: Int): Channel<Int> {
        val channel = Channel<Int>()
        scope.launch {
            for (i in 1..count) {
                channel.send(i)
            }
            // Bug: never calls channel.close()
        }
        return channel
    }

    @kotlinx.coroutines.ExperimentalCoroutinesApi
    fun produceWithAutoClose(count: Int): ReceiveChannel<Int> {
        return scope.produce {
            for (i in 1..count) {
                send(i)
            }
            // produce{} auto-closes on completion
        }
    }

    fun bufferedChannel(capacity: Int, items: List<Int>): Channel<Int> {
        val channel = Channel<Int>(capacity)
        scope.launch {
            for (item in items) {
                channel.send(item)
            }
            channel.close()
        }
        return channel
    }

    fun conflatedChannel(items: List<Int>): Channel<Int> {
        val channel = Channel<Int>(Channel.CONFLATED)
        scope.launch {
            for (item in items) {
                channel.send(item)
            }
            channel.close()
        }
        return channel
    }
}
