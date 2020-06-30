package directives.options

/**
 * See [OptionParser]. This handles a full space-delimited list of directives.options and assembles a list of actual option
 * instances from it
 */
interface MultiOptionParser<Option : CspDirectiveOptions> : OptionParser<Option> {
    /**
     * "out Option" is Kotlin syntax for a covariant type parameter. Here, we don't care what the given
     * [SingleOptionParser]s *actually* have as their type arguments, so long as we can safely assume the type argument
     * "is a" [Option] as far as we are concerned. This makes syntax like used in [SourceOptions] work (each option
     * makes itself, but since those types descend from [SourceOptions], this field can be filled with a list of them).
     *
     * For more information, see https://kotlinlang.org/docs/reference/generics.html
     * Essentially, this just works slightly different from Java and this is a type-safe way of doing it
     */
    val possibleOptions: List<SingleOptionParser<out Option>>
    override fun parseFrom(string: String): List<Option>? = buildList {
        string.split(' ').forEach { s ->
            possibleOptions.firstOrNull { option -> option.parseFrom(s)?.let { addAll(it) } != null }
        }
    }
}