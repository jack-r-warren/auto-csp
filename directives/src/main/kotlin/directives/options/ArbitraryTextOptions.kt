package directives.options

/**
 * Represents arbitrary text as an option. Each non-empty, space delimited option is interpreted as an option. Of
 * course, it is possible to break this (semicolons in particular will break it) but since this is primarily
 * used for the group names provided for reports this tries to be as permissive as possible.
 *
 * Doesn't have a way to be strict since it is arbitrary.
 */
class ArbitraryTextOptions(override val asString: String) : CspDirectiveOptions {
    companion object : OptionParser<ArbitraryTextOptions> {
        override fun parseFrom(string: String): List<ArbitraryTextOptions>? =
            string.split(' ').filterNot(String::isEmpty).map(::ArbitraryTextOptions)
    }
}