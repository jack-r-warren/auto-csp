package directives.util

import directives.CspDirective
import directives.options.CspDirectiveOptions

fun Map<String, CspDirective<out CspDirectiveOptions>>.toPolicyString() = map { (name, directive) ->
    if (directive.options.isEmpty()) name else "$name ${directive.optionsAsString}"
}.joinToString(separator = "; ")