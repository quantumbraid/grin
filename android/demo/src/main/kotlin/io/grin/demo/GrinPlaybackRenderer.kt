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
package io.grin.demo

import android.graphics.Bitmap
import io.grin.lib.GrinBitmapRenderer
import io.grin.lib.GrinFile
import io.grin.lib.GrinPlayer
import io.grin.lib.TestTickScheduler

// Renders playback-derived bitmaps for export snapshots and GIF loops.
class GrinPlaybackRenderer(
    private val bitmapRenderer: GrinBitmapRenderer = GrinBitmapRenderer()
) {
    fun renderSnapshot(grinFile: GrinFile, tick: Long = 0L): Bitmap {
        // Render a single playback frame at the specified tick.
        val player = buildPlayer(grinFile)
        player.seek(tick)
        return bitmapRenderer.render(player.getCurrentFrame())
    }

    fun renderFrames(grinFile: GrinFile, frameCount: Int, tickStep: Long): List<Bitmap> {
        // Render a list of playback frames at fixed tick intervals.
        require(frameCount in 1..60) { "Frame count must be between 1 and 60" }
        require(tickStep >= 1L) { "Tick step must be at least 1" }

        val player = buildPlayer(grinFile)
        val frames = mutableListOf<Bitmap>()
        var tick = 0L
        repeat(frameCount) {
            player.seek(tick)
            frames.add(bitmapRenderer.render(player.getCurrentFrame()))
            tick += tickStep
        }
        return frames
    }

    private fun buildPlayer(grinFile: GrinFile): GrinPlayer {
        // Build a player with a deterministic scheduler for offline rendering.
        val player = GrinPlayer(schedulerFactory = { TestTickScheduler() })
        player.load(grinFile)
        return player
    }
}
