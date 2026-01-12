package io.grin.lib

data class GrinValidationResult(
    val ok: Boolean,
    val errors: List<String>,
    val warnings: List<String>
)

enum class ValidationMode {
    STRICT,
    PERMISSIVE
}

data class ValidationReport(
    val ok: Boolean,
    val errors: List<String>,
    val warnings: List<String>,
    val info: List<String>
)
