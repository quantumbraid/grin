package io.grin.lib

import kotlin.math.abs
import kotlin.math.roundToInt

class NopOpcode : Opcode {
    override fun getId(): Int = BaseOpcodes.NOP
    override fun getName(): String = "NOP"
    override fun apply(pixel: GrinPixel, tick: Long, timing: Int) {}
    override fun getMaxCpuCost(): Int = 1
    override fun requiresState(): Boolean = false
}

class FadeInOpcode : Opcode {
    override fun getId(): Int = BaseOpcodes.FADE_IN
    override fun getName(): String = "FADE_IN"
    override fun apply(pixel: GrinPixel, tick: Long, timing: Int) {
        val level = TimingInterpreter.evaluate(timing, tick)
        pixel.a = clampByte((pixel.a * level).roundToInt())
    }
    override fun getMaxCpuCost(): Int = 1
    override fun requiresState(): Boolean = false
}

class FadeOutOpcode : Opcode {
    override fun getId(): Int = BaseOpcodes.FADE_OUT
    override fun getName(): String = "FADE_OUT"
    override fun apply(pixel: GrinPixel, tick: Long, timing: Int) {
        val level = 1.0 - TimingInterpreter.evaluate(timing, tick)
        pixel.a = clampByte((pixel.a * level).roundToInt())
    }
    override fun getMaxCpuCost(): Int = 1
    override fun requiresState(): Boolean = false
}

class PulseOpcode : Opcode {
    override fun getId(): Int = BaseOpcodes.PULSE
    override fun getName(): String = "PULSE"
    override fun apply(pixel: GrinPixel, tick: Long, timing: Int) {
        val level = TimingInterpreter.evaluate(timing, tick)
        pixel.a = clampByte((pixel.a * level).roundToInt())
    }
    override fun getMaxCpuCost(): Int = 1
    override fun requiresState(): Boolean = false
}

class ShiftROpcode : Opcode {
    override fun getId(): Int = BaseOpcodes.SHIFT_R
    override fun getName(): String = "SHIFT_R"
    override fun apply(pixel: GrinPixel, tick: Long, timing: Int) {
        pixel.r = shiftChannel(pixel.r, tick, timing)
    }
    override fun getMaxCpuCost(): Int = 1
    override fun requiresState(): Boolean = false
}

class ShiftGOpcode : Opcode {
    override fun getId(): Int = BaseOpcodes.SHIFT_G
    override fun getName(): String = "SHIFT_G"
    override fun apply(pixel: GrinPixel, tick: Long, timing: Int) {
        pixel.g = shiftChannel(pixel.g, tick, timing)
    }
    override fun getMaxCpuCost(): Int = 1
    override fun requiresState(): Boolean = false
}

class ShiftBOpcode : Opcode {
    override fun getId(): Int = BaseOpcodes.SHIFT_B
    override fun getName(): String = "SHIFT_B"
    override fun apply(pixel: GrinPixel, tick: Long, timing: Int) {
        pixel.b = shiftChannel(pixel.b, tick, timing)
    }
    override fun getMaxCpuCost(): Int = 1
    override fun requiresState(): Boolean = false
}

class ShiftAOpcode : Opcode {
    override fun getId(): Int = BaseOpcodes.SHIFT_A
    override fun getName(): String = "SHIFT_A"
    override fun apply(pixel: GrinPixel, tick: Long, timing: Int) {
        pixel.a = shiftChannel(pixel.a, tick, timing)
    }
    override fun getMaxCpuCost(): Int = 1
    override fun requiresState(): Boolean = false
}

class InvertOpcode : Opcode {
    override fun getId(): Int = BaseOpcodes.INVERT
    override fun getName(): String = "INVERT"
    override fun apply(pixel: GrinPixel, tick: Long, timing: Int) {
        pixel.r = 255 - pixel.r
        pixel.g = 255 - pixel.g
        pixel.b = 255 - pixel.b
    }
    override fun getMaxCpuCost(): Int = 1
    override fun requiresState(): Boolean = false
}

class RotateHueOpcode : Opcode {
    override fun getId(): Int = BaseOpcodes.ROTATE_HUE
    override fun getName(): String = "ROTATE_HUE"
    override fun apply(pixel: GrinPixel, tick: Long, timing: Int) {
        val rotation = TimingInterpreter.evaluate(timing, tick) * 360.0

        val rNorm = pixel.r / 255.0
        val gNorm = pixel.g / 255.0
        val bNorm = pixel.b / 255.0
        val max = maxOf(rNorm, gNorm, bNorm)
        val min = minOf(rNorm, gNorm, bNorm)
        val delta = max - min
        val l = (max + min) / 2
        var h = 0.0
        var s = 0.0

        if (delta != 0.0) {
            s = delta / (1 - abs(2 * l - 1))
            h = when (max) {
                rNorm -> ((gNorm - bNorm) / delta) % 6
                gNorm -> ((bNorm - rNorm) / delta) + 2
                else -> ((rNorm - gNorm) / delta) + 4
            }
            h *= 60
            if (h < 0) {
                h += 360
            }
        }

        var newHue = (h + rotation) % 360.0
        if (newHue < 0) {
            newHue += 360.0
        }

        val c = (1 - abs(2 * l - 1)) * s
        val hPrime = newHue / 60.0
        val x = c * (1 - abs((hPrime % 2) - 1))

        var r1 = 0.0
        var g1 = 0.0
        var b1 = 0.0
        when {
            hPrime < 1 -> {
                r1 = c
                g1 = x
            }
            hPrime < 2 -> {
                r1 = x
                g1 = c
            }
            hPrime < 3 -> {
                g1 = c
                b1 = x
            }
            hPrime < 4 -> {
                g1 = x
                b1 = c
            }
            hPrime < 5 -> {
                r1 = x
                b1 = c
            }
            else -> {
                r1 = c
                b1 = x
            }
        }

        val m = l - c / 2
        pixel.r = clampByte(((r1 + m) * 255).roundToInt())
        pixel.g = clampByte(((g1 + m) * 255).roundToInt())
        pixel.b = clampByte(((b1 + m) * 255).roundToInt())
    }
    override fun getMaxCpuCost(): Int = 3
    override fun requiresState(): Boolean = false
}

class LockOpcode : Opcode {
    override fun getId(): Int = BaseOpcodes.LOCK
    override fun getName(): String = "LOCK"
    override fun apply(pixel: GrinPixel, tick: Long, timing: Int) {
        pixel.c = pixel.c or GrinControlByte.LOCK_MASK
    }
    override fun getMaxCpuCost(): Int = 1
    override fun requiresState(): Boolean = false
}

class UnlockOpcode : Opcode {
    override fun getId(): Int = BaseOpcodes.UNLOCK
    override fun getName(): String = "UNLOCK"
    override fun apply(pixel: GrinPixel, tick: Long, timing: Int) {
        pixel.c = pixel.c and GrinControlByte.LOCK_MASK.inv()
    }
    override fun getMaxCpuCost(): Int = 1
    override fun requiresState(): Boolean = false
}

class ToggleLockOpcode : Opcode {
    override fun getId(): Int = BaseOpcodes.TOGGLE_LOCK
    override fun getName(): String = "TOGGLE_LOCK"
    override fun apply(pixel: GrinPixel, tick: Long, timing: Int) {
        pixel.c = pixel.c xor GrinControlByte.LOCK_MASK
    }
    override fun getMaxCpuCost(): Int = 1
    override fun requiresState(): Boolean = false
}

private fun shiftChannel(value: Int, tick: Long, timing: Int): Int {
    val wave = TimingInterpreter.evaluate(timing, tick)
    val delta = ((wave * 2.0) - 1.0) * 255.0
    return clampByte((value + delta).roundToInt())
}

private fun clampByte(value: Int): Int {
    return when {
        value < 0 -> 0
        value > 255 -> 255
        else -> value
    }
}
