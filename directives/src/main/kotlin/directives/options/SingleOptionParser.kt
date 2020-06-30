package directives.options

/**
 * See [OptionParser]. This takes some string that may be an option and tries to parse it
 */
interface SingleOptionParser<Option : CspDirectiveOptions> : OptionParser<Option> {
    val pattern: Regex
    fun constructFrom(matcher: MatchResult): Option
    override fun parseFrom(string: String): List<Option>? =
        pattern.matchEntire(string)?.let { listOf(constructFrom(it)) }
}