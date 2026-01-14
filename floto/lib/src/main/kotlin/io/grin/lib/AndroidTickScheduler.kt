/*
 * MIT License
 *
 * Copyright (c) 2025 GRIN Project Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
