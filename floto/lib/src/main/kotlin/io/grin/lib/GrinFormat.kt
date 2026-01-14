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

// Spec references: §§4, 4.1, 7.2-7.4 (grin_technical_specification_v_2.md)
object GrinFormat {
    val MAGIC_BYTES: ByteArray = byteArrayOf(
        0x47.toByte(),
        0x52.toByte(),
        0x49.toByte(),
        0x4E.toByte()
    )
    const val VERSION_MAJOR: Int = 0x00
    const val VERSION_MINOR: Int = 0x00
    const val HEADER_SIZE_BYTES: Int = 128
    const val PIXEL_SIZE_BYTES: Int = 5
    const val MAX_RULE_COUNT: Int = 16
    const val RULES_BLOCK_SIZE: Int = 64
    const val RULE_ENTRY_SIZE: Int = 4
    const val TIMING_SEMANTICS: String = "reader-defined oscillator control"
}

object GrinHeaderLayout {
    const val MAGIC_OFFSET: Int = 0
    const val MAGIC_SIZE: Int = 4
    const val VERSION_MAJOR_OFFSET: Int = 4
    const val VERSION_MAJOR_SIZE: Int = 1
    const val VERSION_MINOR_OFFSET: Int = 5
    const val VERSION_MINOR_SIZE: Int = 1
    const val HEADER_SIZE_OFFSET: Int = 6
    const val HEADER_SIZE_SIZE: Int = 2
    const val WIDTH_OFFSET: Int = 8
    const val WIDTH_SIZE: Int = 4
    const val HEIGHT_OFFSET: Int = 12
    const val HEIGHT_SIZE: Int = 4
    const val TICK_MICROS_OFFSET: Int = 16
    const val TICK_MICROS_SIZE: Int = 4
    const val RULE_COUNT_OFFSET: Int = 20
    const val RULE_COUNT_SIZE: Int = 1
    const val OPCODE_SET_ID_OFFSET: Int = 21
    const val OPCODE_SET_ID_SIZE: Int = 1
    const val FLAGS_OFFSET: Int = 22
    const val FLAGS_SIZE: Int = 2
    const val PIXEL_DATA_LENGTH_OFFSET: Int = 24
    const val PIXEL_DATA_LENGTH_SIZE: Int = 8
    const val FILE_LENGTH_OFFSET: Int = 32
    const val FILE_LENGTH_SIZE: Int = 8
    const val PIXEL_DATA_OFFSET_OFFSET: Int = 40
    const val PIXEL_DATA_OFFSET_SIZE: Int = 8
    const val RESERVED_A_OFFSET: Int = 48
    const val RESERVED_A_SIZE: Int = 8
    const val RESERVED_B_OFFSET: Int = 56
    const val RESERVED_B_SIZE: Int = 8
    const val RULES_BLOCK_OFFSET: Int = 64
    const val RULES_BLOCK_SIZE: Int = GrinFormat.RULES_BLOCK_SIZE
}

object GrinRuleLayout {
    const val GROUP_MASK_OFFSET: Int = 0
    const val GROUP_MASK_SIZE: Int = 2
    const val OPCODE_OFFSET: Int = 2
    const val OPCODE_SIZE: Int = 1
    const val TIMING_OFFSET: Int = 3
    const val TIMING_SIZE: Int = 1

    fun groupMaskTargetsGroup(groupMask: Int, groupId: Int): Boolean {
        val shift = groupId and 0x0F
        return (groupMask and (1 shl shift)) != 0
    }
}

object GrinPixelLayout {
    const val R_OFFSET: Int = 0
    const val G_OFFSET: Int = 1
    const val B_OFFSET: Int = 2
    const val A_OFFSET: Int = 3
    const val C_OFFSET: Int = 4
}

object GrinControlByte {
    const val GROUP_ID_MASK: Int = 0x0F
    const val RESERVED_MASK: Int = 0x70
    const val LOCK_MASK: Int = 0x80

    fun getGroupId(controlByte: Int): Int = controlByte and GROUP_ID_MASK

    fun isLocked(controlByte: Int): Boolean = (controlByte and LOCK_MASK) != 0

    fun setGroupId(controlByte: Int, groupId: Int): Int {
        val cleared = controlByte and GROUP_ID_MASK.inv()
        return cleared or (groupId and GROUP_ID_MASK)
    }

    fun setLocked(controlByte: Int, locked: Boolean): Int {
        return if (locked) {
            controlByte or LOCK_MASK
        } else {
            controlByte and LOCK_MASK.inv()
        }
    }
}
