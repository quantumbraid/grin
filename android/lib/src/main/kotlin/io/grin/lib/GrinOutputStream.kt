package io.grin.lib

import java.io.FilterOutputStream
import java.io.OutputStream

class GrinOutputStream(output: OutputStream) : FilterOutputStream(output) {
    fun writeHeader(header: GrinHeader) {
        writeHeader(header, this)
    }

    fun writePixelData(pixels: List<GrinPixel>) {
        writePixelData(pixels, this)
    }

    fun writeRulesBlock(rules: List<GrinRule>, ruleCount: Int) {
        val block = io.grin.lib.writeRulesBlock(rules, ruleCount)
        write(block)
    }
}
