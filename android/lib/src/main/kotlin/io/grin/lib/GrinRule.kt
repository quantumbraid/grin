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

import java.nio.ByteBuffer
import java.nio.ByteOrder

class GrinRule(
    groupMask: Int,
    opcode: Int,
    timing: Int
) {
    var groupMask: Int = groupMask and 0xFFFF
    var opcode: Int = opcode and 0xFF
    var timing: Int = timing and 0xFF

    fun targetsGroup(groupId: Int): Boolean {
        return GrinRuleLayout.groupMaskTargetsGroup(groupMask, groupId)
    }

    fun serialize(): ByteArray {
        val buffer = ByteBuffer.allocate(GrinFormat.RULE_ENTRY_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putShort(GrinRuleLayout.GROUP_MASK_OFFSET, (groupMask and 0xFFFF).toShort())
        buffer.put(GrinRuleLayout.OPCODE_OFFSET, opcode.toByte())
        buffer.put(GrinRuleLayout.TIMING_OFFSET, timing.toByte())
        return buffer.array()
    }

    companion object {
        fun deserialize(bytes: ByteArray): GrinRule {
            require(bytes.size == GrinFormat.RULE_ENTRY_SIZE) {
                "GrinRule requires exactly ${GrinFormat.RULE_ENTRY_SIZE} bytes"
            }
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val groupMask = buffer.getShort(GrinRuleLayout.GROUP_MASK_OFFSET).toInt() and 0xFFFF
            val opcode = buffer.get(GrinRuleLayout.OPCODE_OFFSET).toInt() and 0xFF
            val timing = buffer.get(GrinRuleLayout.TIMING_OFFSET).toInt() and 0xFF
            return GrinRule(groupMask, opcode, timing)
        }
    }
}
