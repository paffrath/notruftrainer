package koeln.paffrath.notruftrainer

data class NumberMapping(
    val dialedNumber: String,
    val actualTarget: String,
    val displayName: String,
)

val DEFAULT_MAPPINGS: List<NumberMapping> = listOf(
    NumberMapping(
        dialedNumber = "112",
        actualTarget = BuildConfig.TRAINING_NUMBER,
        displayName = "Notruf 112",
    ),
)

fun findMapping(input: String): NumberMapping? =
    DEFAULT_MAPPINGS.firstOrNull { it.dialedNumber == input }
