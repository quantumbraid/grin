package io.grin.lib

class TestTickScheduler : TickScheduler {
    private var tickCallback: TickCallback? = null
    private var currentTick: Long = 0
    private var running = false

    override fun start() {
        running = true
    }

    override fun stop() {
        running = false
    }

    override fun setTickCallback(cb: TickCallback) {
        tickCallback = cb
    }

    override fun getCurrentTick(): Long = currentTick

    fun advance(ticks: Long = 1) {
        if (!running) {
            return
        }
        var index = 0L
        while (index < ticks) {
            currentTick = if (currentTick == Long.MAX_VALUE) 0L else currentTick + 1
            tickCallback?.invoke(currentTick)
            index += 1
        }
    }
}
