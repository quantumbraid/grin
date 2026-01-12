package io.grin.lib

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor

enum class WaveformType {
    SQUARE,
    TRIANGLE,
    SINE,
    SAWTOOTH
}

object TimingInterpreter {
    fun getPeriod(timing: Int): Int {
        return (timing and 0x0F) + 1
    }

    fun getWaveform(timing: Int): WaveformType {
        return when ((timing ushr 4) and 0x03) {
            0 -> WaveformType.SQUARE
            1 -> WaveformType.TRIANGLE
            2 -> WaveformType.SINE
            else -> WaveformType.SAWTOOTH
        }
    }

    fun getPhaseOffset(timing: Int): Int {
        return (timing ushr 6) and 0x03
    }

    fun evaluate(timing: Int, tick: Long): Double {
        val period = getPeriod(timing).toDouble()
        val phaseOffset = getPhaseOffset(timing) / 4.0
        val phase = (tick / period) + phaseOffset
        val position = phase - floor(phase)
        return when (getWaveform(timing)) {
            WaveformType.SQUARE -> if (position < 0.5) 0.0 else 1.0
            WaveformType.TRIANGLE -> if (position < 0.5) position * 2 else 2 - position * 2
            WaveformType.SINE -> 0.5 - 0.5 * cos(2 * PI * position)
            WaveformType.SAWTOOTH -> position
        }
    }
}
