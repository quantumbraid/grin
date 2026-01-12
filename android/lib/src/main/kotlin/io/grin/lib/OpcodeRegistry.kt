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

object OpcodeRegistry {
    private val baseOpcodes: Map<Int, Opcode> = mapOf(
        BaseOpcodes.NOP to NopOpcode(),
        BaseOpcodes.FADE_IN to FadeInOpcode(),
        BaseOpcodes.FADE_OUT to FadeOutOpcode(),
        BaseOpcodes.PULSE to PulseOpcode(),
        BaseOpcodes.SHIFT_R to ShiftROpcode(),
        BaseOpcodes.SHIFT_G to ShiftGOpcode(),
        BaseOpcodes.SHIFT_B to ShiftBOpcode(),
        BaseOpcodes.SHIFT_A to ShiftAOpcode(),
        BaseOpcodes.INVERT to InvertOpcode(),
        BaseOpcodes.ROTATE_HUE to RotateHueOpcode(),
        BaseOpcodes.LOCK to LockOpcode(),
        BaseOpcodes.UNLOCK to UnlockOpcode(),
        BaseOpcodes.TOGGLE_LOCK to ToggleLockOpcode()
    )

    fun getOpcode(opcodeSetId: Int, opcodeId: Int): Opcode {
        require(opcodeSetId == 0) { "Unknown OpcodeSetId $opcodeSetId" }
        return baseOpcodes[opcodeId]
            ?: throw IllegalArgumentException("Unknown opcode $opcodeId for base set")
    }

    fun isValidOpcode(opcodeSetId: Int, opcodeId: Int): Boolean {
        if (opcodeSetId != 0) {
            return false
        }
        return baseOpcodes.containsKey(opcodeId)
    }

    fun listOpcodes(opcodeSetId: Int): List<Opcode> {
        if (opcodeSetId != 0) {
            return emptyList()
        }
        return baseOpcodes.values.toList()
    }
}
