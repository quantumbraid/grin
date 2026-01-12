package io.grin.lib

class GrinPlayer(
    private val schedulerFactory: (Long) -> TickScheduler = { tickMicros ->
        AndroidTickScheduler(tickMicros)
    },
    private val ruleEngine: RuleEngine = RuleEngine(),
    private val opcodeRegistry: OpcodeRegistry = OpcodeRegistry,
    private val onFrameRendered: (() -> Unit)? = null
) {
    private var image: GrinImage? = null
    private var scheduler: TickScheduler? = null
    private var state: PlaybackState? = null
    private var displayBuffer: DisplayBuffer? = null
    private var controlBytes: IntArray = IntArray(0)

    fun load(file: GrinFile) {
        val header = file.header
        val width = safeInt(header.width, "Width")
        val height = safeInt(header.height, "Height")

        image = GrinImage(header, file.pixels.toMutableList(), file.rules)
        displayBuffer = DisplayBuffer(width, height)
        state = PlaybackState(displayBuffer!!)
        controlBytes = IntArray(file.pixels.size) { index ->
            file.pixels[index].c and GrinControlByte.RESERVED_MASK.inv() and 0xFF
        }

        scheduler?.stop()
        scheduler = schedulerFactory(header.tickMicros).also { sched ->
            sched.setTickCallback { tick -> onTick(tick) }
        }
    }

    fun play() {
        val currentState = state ?: throw IllegalStateException("GrinPlayer.load() must be called before play()")
        val currentScheduler = scheduler ?: throw IllegalStateException("Scheduler not initialized")
        if (currentState.isPlaying) {
            return
        }
        currentState.isPlaying = true
        currentScheduler.start()
    }

    fun pause() {
        val currentState = state ?: return
        val currentScheduler = scheduler ?: return
        currentState.isPlaying = false
        currentScheduler.stop()
    }

    fun stop() {
        state?.reset()
        pause()
    }

    fun seek(tick: Long) {
        require(tick >= 0L) { "Tick must be non-negative" }
        state?.currentTick = tick
        renderFrame(tick)
    }

    fun getCurrentFrame(): DisplayBuffer {
        return displayBuffer ?: throw IllegalStateException("GrinPlayer.load() must be called before getCurrentFrame()")
    }

    fun isPlaying(): Boolean {
        return state?.isPlaying ?: false
    }

    private fun onTick(tick: Long) {
        state?.currentTick = tick
        renderFrame(tick)
    }

    private fun renderFrame(tick: Long) {
        val currentImage = image ?: return
        val buffer = displayBuffer ?: return
        val output = buffer.rgbaData
        val opcodeSetId = currentImage.header.opcodeSetId
        val activeRules = ruleEngine.evaluateRules(currentImage, tick)

        currentImage.pixels.forEachIndexed { index, source ->
            val control = controlBytes[index]
            val outputIndex = index * 4

            if ((control and GrinControlByte.LOCK_MASK) != 0) {
                output[outputIndex] = source.r.toByte()
                output[outputIndex + 1] = source.g.toByte()
                output[outputIndex + 2] = source.b.toByte()
                output[outputIndex + 3] = source.a.toByte()
                return@forEachIndexed
            }

            val working = GrinPixel(source.r, source.g, source.b, source.a, control)
            val groupId = control and GrinControlByte.GROUP_ID_MASK

            for (activeRule in activeRules) {
                if (activeRule.rule.targetsGroup(groupId)) {
                    val opcode = opcodeRegistry.getOpcode(opcodeSetId, activeRule.rule.opcode)
                    opcode.apply(working, tick, activeRule.rule.timing)
                }
            }

            controlBytes[index] = working.c and GrinControlByte.RESERVED_MASK.inv() and 0xFF
            output[outputIndex] = working.r.toByte()
            output[outputIndex + 1] = working.g.toByte()
            output[outputIndex + 2] = working.b.toByte()
            output[outputIndex + 3] = working.a.toByte()
        }
        onFrameRendered?.invoke()
    }
}

private fun safeInt(value: Long, label: String): Int {
    require(value <= Int.MAX_VALUE.toLong()) { "$label exceeds Int range" }
    require(value >= 0L) { "$label must be non-negative" }
    return value.toInt()
}
