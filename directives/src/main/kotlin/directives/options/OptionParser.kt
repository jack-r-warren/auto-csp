package directives.options

/**
 * An interface for the companion objects of [CspDirectiveOptions] that handles creating the instances.
 *
 * For convenience, there are subinterfaces of this that handle the actual semantics of parsing:
 * - [SingleOptionParser] takes a string that might be the option and tries to parse it
 * - [MultiOptionParser] tries each space-delimited option in the given string against a list of [SingleOptionParser]s,
 *   using the first of each that hits
 */
interface OptionParser<Option : CspDirectiveOptions> {
    fun parseFrom(string: String): List<Option>?

    /**
     * The set of directives.options that will cause directives using them to be as strict as possible. This a shortcut to easily
     * generate a "strict as possible" CSP.
     *
     * By default, set to empty, which may be essentially a null operation ([ArbitraryTextOptions]) or may actually be
     * a strict setting ([SandboxOptions], [MimeTypeOptions]).
     */
    val strictest: List<Option>
        get() = listOf()
}