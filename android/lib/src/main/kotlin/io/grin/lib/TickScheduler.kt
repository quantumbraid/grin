package io.grin.lib

typealias TickCallback = (Long) -> Unit

interface TickScheduler {
    fun start()
    fun stop()
    fun setTickCallback(cb: TickCallback)
    fun getCurrentTick(): Long
}
