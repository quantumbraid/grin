package io.grin.lib

interface Opcode {
    fun getId(): Int
    fun getName(): String
    fun apply(pixel: GrinPixel, tick: Long, timing: Int)
    fun getMaxCpuCost(): Int
    fun requiresState(): Boolean
}
