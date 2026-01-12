package io.grin.lib

import android.view.Choreographer

open class AndroidTickScheduler(private val tickMicros: Long) : TickScheduler {
    private var tickCallback: TickCallback? = null
    private var currentTick: Long = 0
    private var accumulatorMicros: Long = 0
    private var lastFrameNanos: Long = 0
    private var running = false

    private val frameCallback: Choreographer.FrameCallback =
        object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (!running) {
                    return
                }
                if (lastFrameNanos == 0L) {
                    lastFrameNanos = frameTimeNanos
                }
                val deltaMicros = (frameTimeNanos - lastFrameNanos) / 1000
                lastFrameNanos = frameTimeNanos
                accumulatorMicros += deltaMicros

                val step = if (tickMicros <= 0) 1L else tickMicros
                while (accumulatorMicros >= step) {
                    accumulatorMicros -= step
                    currentTick = nextTick(currentTick)
                    tickCallback?.invoke(currentTick)
                }

                Choreographer.getInstance().postFrameCallback(this)
            }
        }

    override fun start() {
        if (running) {
            return
        }
        running = true
        lastFrameNanos = 0
        accumulatorMicros = 0
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    override fun stop() {
        if (!running) {
            return
        }
        running = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    override fun setTickCallback(cb: TickCallback) {
        tickCallback = cb
    }

    override fun getCurrentTick(): Long = currentTick
}

private fun nextTick(currentTick: Long): Long {
    return if (currentTick == Long.MAX_VALUE) 0L else currentTick + 1
}
