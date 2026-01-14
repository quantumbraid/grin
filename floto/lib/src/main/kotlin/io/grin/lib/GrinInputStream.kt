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

import java.io.FilterInputStream
import java.io.InputStream

class GrinInputStream(input: InputStream) : FilterInputStream(input) {
    fun readHeader(): GrinHeaderReadResult {
        return readHeader(this)
    }

    fun readPixelData(width: Long, height: Long): GrinPixelReadResult {
        return readPixelData(this, width, height)
    }

    fun readRulesBlock(ruleCount: Int, opcodeSetId: Int): GrinRulesReadResult {
        val buffer = ByteArray(GrinFormat.RULES_BLOCK_SIZE)
        readFully(buffer)
        return readRulesBlock(buffer, ruleCount, opcodeSetId)
    }

    private fun readFully(buffer: ByteArray) {
        var offset = 0
        while (offset < buffer.size) {
            val read = read(buffer, offset, buffer.size - offset)
            if (read == -1) {
                throw IllegalArgumentException("Unexpected end of stream")
            }
            offset += read
        }
    }
}
