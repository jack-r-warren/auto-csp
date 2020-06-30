package directives.options

/**
 * A [CspDirectiveOptions] class for when a [directives.CspDirective] has no directives.options that can be set
 */
abstract class NoOptions : CspDirectiveOptions {
    companion object : OptionParser<NoOptions> {
        override fun parseFrom(string: String): List<NoOptions>? = null
    }
}