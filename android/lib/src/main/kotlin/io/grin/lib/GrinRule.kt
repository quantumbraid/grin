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
