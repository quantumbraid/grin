package io.grin.lib

class PlaybackState(val displayBuffer: DisplayBuffer) {
    var currentTick: Long = 0
    var isPlaying: Boolean = false
    var tickAccumulatorMicros: Long = 0

    fun reset() {
        currentTick = 0
        tickAccumulatorMicros = 0
        isPlaying = false
    }
}
