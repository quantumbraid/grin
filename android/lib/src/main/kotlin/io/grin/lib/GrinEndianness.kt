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

fun writeUint16LE(value: Int): ByteArray {
    return byteArrayOf(
        (value and 0xFF).toByte(),
        ((value ushr 8) and 0xFF).toByte()
    )
}

fun writeUint32LE(value: Long): ByteArray {
    return byteArrayOf(
        (value and 0xFF).toByte(),
        ((value ushr 8) and 0xFF).toByte(),
        ((value ushr 16) and 0xFF).toByte(),
        ((value ushr 24) and 0xFF).toByte()
    )
}

fun writeUint64LE(value: Long): ByteArray {
    return byteArrayOf(
        (value and 0xFF).toByte(),
        ((value ushr 8) and 0xFF).toByte(),
        ((value ushr 16) and 0xFF).toByte(),
        ((value ushr 24) and 0xFF).toByte(),
        ((value ushr 32) and 0xFF).toByte(),
        ((value ushr 40) and 0xFF).toByte(),
        ((value ushr 48) and 0xFF).toByte(),
        ((value ushr 56) and 0xFF).toByte()
    )
}

fun readUint16LE(bytes: ByteArray, offset: Int): Int {
    require(offset >= 0 && offset + 2 <= bytes.size) { "readUint16LE out of range" }
    val b0 = bytes[offset].toInt() and 0xFF
    val b1 = bytes[offset + 1].toInt() and 0xFF
    return b0 or (b1 shl 8)
}

fun readUint32LE(bytes: ByteArray, offset: Int): Long {
    require(offset >= 0 && offset + 4 <= bytes.size) { "readUint32LE out of range" }
    val b0 = bytes[offset].toLong() and 0xFF
    val b1 = bytes[offset + 1].toLong() and 0xFF
    val b2 = bytes[offset + 2].toLong() and 0xFF
    val b3 = bytes[offset + 3].toLong() and 0xFF
    return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
}

fun readUint64LE(bytes: ByteArray, offset: Int): Long {
    require(offset >= 0 && offset + 8 <= bytes.size) { "readUint64LE out of range" }
    var result = 0L
    var shift = 0
    var index = 0
    while (index < 8) {
        result = result or ((bytes[offset + index].toLong() and 0xFF) shl shift)
        shift += 8
        index += 1
    }
    return result
}
