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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class OpcodesTest {
    @Test
    fun nopOpcodeLeavesPixelUnchanged() {
        val pixel = GrinPixel(10, 20, 30, 40, 0)
        NopOpcode().apply(pixel, 1, 0x01)
        assertEquals(10, pixel.r)
        assertEquals(20, pixel.g)
        assertEquals(30, pixel.b)
        assertEquals(40, pixel.a)
        assertEquals(0, pixel.c)
    }

    @Test
    fun fadeInCanZeroAlpha() {
        val pixel = GrinPixel(10, 20, 30, 200, 0)
        FadeInOpcode().apply(pixel, 0, 0x01)
        assertEquals(0, pixel.a)
    }

    @Test
    fun fadeOutCanZeroAlpha() {
        val pixel = GrinPixel(10, 20, 30, 200, 0)
        FadeOutOpcode().apply(pixel, 1, 0x01)
        assertEquals(0, pixel.a)
    }

    @Test
    fun shiftOpcodesClampChannels() {
        val rPixel = GrinPixel(10, 20, 30, 40, 0)
        val gPixel = GrinPixel(10, 20, 30, 40, 0)
        val bPixel = GrinPixel(10, 20, 30, 40, 0)
        val aPixel = GrinPixel(10, 20, 30, 40, 0)

        ShiftROpcode().apply(rPixel, 1, 0x01)
        ShiftGOpcode().apply(gPixel, 0, 0x01)
        ShiftBOpcode().apply(bPixel, 1, 0x01)
        ShiftAOpcode().apply(aPixel, 0, 0x01)

        assertEquals(255, rPixel.r)
        assertEquals(0, gPixel.g)
        assertEquals(255, bPixel.b)
        assertEquals(0, aPixel.a)
    }

    @Test
    fun invertOpcodeFlipsRgb() {
        val pixel = GrinPixel(10, 20, 30, 200, 0)
        InvertOpcode().apply(pixel, 0, 0)
        assertEquals(245, pixel.r)
        assertEquals(235, pixel.g)
        assertEquals(225, pixel.b)
        assertEquals(200, pixel.a)
    }

    @Test
    fun rotateHueOpcodeRotatesRedToCyan() {
        val pixel = GrinPixel(255, 0, 0, 255, 0)
        RotateHueOpcode().apply(pixel, 1, 0x31)
        assertChannelApprox(0, pixel.r)
        assertChannelApprox(255, pixel.g)
        assertChannelApprox(255, pixel.b)
    }

    @Test
    fun lockOpcodesToggleLockBit() {
        val pixel = GrinPixel(10, 20, 30, 40, 0)
        LockOpcode().apply(pixel, 0, 0)
        assertEquals(GrinControlByte.LOCK_MASK, pixel.c and GrinControlByte.LOCK_MASK)

        UnlockOpcode().apply(pixel, 0, 0)
        assertEquals(0, pixel.c and GrinControlByte.LOCK_MASK)

        ToggleLockOpcode().apply(pixel, 0, 0)
        assertEquals(GrinControlByte.LOCK_MASK, pixel.c and GrinControlByte.LOCK_MASK)
    }

    private fun assertChannelApprox(expected: Int, actual: Int, tolerance: Int = 1) {
        assertTrue(abs(expected - actual) <= tolerance)
    }
}
