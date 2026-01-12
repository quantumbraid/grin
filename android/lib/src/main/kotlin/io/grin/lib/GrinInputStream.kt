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
