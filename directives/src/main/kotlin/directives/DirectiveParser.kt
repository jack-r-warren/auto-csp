package directives

import directives.options.CspDirectiveOptions
import directives.options.OptionParser

/**
 * An interface implemented by the companion objects of [CspDirective] classes. This combo means that those classes
 * statically has this interface available and has the [CspDirective] abstract class available on the instance.
 *
 * Practically, this means you can say `BaseUri.name` but not `BaseUri.directives.options` (you'd have to construct the object for
 * that).
 */
interface DirectiveParser<Options : CspDirectiveOptions, Directive : CspDirective<Options>> {
    val name: String
    val canBeInMetaElement: Boolean
        get() = true
    val canBeInHeader: Boolean
        get() = true
    val canBeInReportOnlyHeader: Boolean
        get() = true
    val optionParser: OptionParser<Options>
    val directiveConstructor: (List<Options>) -> Directive
    fun parseFrom(string: String): Directive? =
        if (string.startsWith(name)) directiveConstructor(
            optionParser.parseFrom(string.substringAfter(name)) ?: listOf()
        )
        else null
    fun makeStrict(): Directive = directiveConstructor(optionParser.strictest)
}