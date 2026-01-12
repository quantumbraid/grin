package io.grin.lib

data class ActiveRule(
    val index: Int,
    val rule: GrinRule
)

private const val ACTIVE_THRESHOLD = 0.5

class RuleEngine {
    fun evaluateRules(image: GrinImage, tick: Long): List<ActiveRule> {
        val active = mutableListOf<ActiveRule>()
        image.rules.forEachIndexed { index, rule ->
            val level = TimingInterpreter.evaluate(rule.timing, tick)
            if (level > ACTIVE_THRESHOLD) {
                active.add(ActiveRule(index, rule))
            }
        }
        return active
    }
}
