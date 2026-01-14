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

class GrinPixel(
    r: Int,
    g: Int,
    b: Int,
    a: Int,
    c: Int
) {
    var r: Int = r and 0xFF
    var g: Int = g and 0xFF
    var b: Int = b and 0xFF
    var a: Int = a and 0xFF
    var c: Int = c and 0xFF

    fun getGroupId(): Int = GrinControlByte.getGroupId(c)

    fun isLocked(): Boolean = GrinControlByte.isLocked(c)

    fun setGroupId(groupId: Int) {
        c = GrinControlByte.setGroupId(c, groupId) and 0xFF
    }

    fun setLocked(locked: Boolean) {
        c = GrinControlByte.setLocked(c, locked) and 0xFF
    }

    fun toBytes(): ByteArray {
        return byteArrayOf(
            r.toByte(),
            g.toByte(),
            b.toByte(),
            a.toByte(),
            c.toByte()
        )
    }

    companion object {
        fun fromBytes(bytes: ByteArray): GrinPixel {
            require(bytes.size == GrinFormat.PIXEL_SIZE_BYTES) {
                "GrinPixel requires exactly ${GrinFormat.PIXEL_SIZE_BYTES} bytes"
            }
            return GrinPixel(
                bytes[GrinPixelLayout.R_OFFSET].toInt() and 0xFF,
                bytes[GrinPixelLayout.G_OFFSET].toInt() and 0xFF,
                bytes[GrinPixelLayout.B_OFFSET].toInt() and 0xFF,
                bytes[GrinPixelLayout.A_OFFSET].toInt() and 0xFF,
                bytes[GrinPixelLayout.C_OFFSET].toInt() and 0xFF
            )
        }
    }
}
