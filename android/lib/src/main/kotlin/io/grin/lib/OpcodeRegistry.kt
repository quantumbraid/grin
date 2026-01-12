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
