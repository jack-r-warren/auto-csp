package directives.options

/**
 * Represents a set of MIME types as directives.options. Does respect parameters in content types, but this requires relying on
 * the "no spaces" quality of MIME types to properly split the "; " between different CSP directives.
 *
 * "Strict" in this context means nothing specifically allowed, so the default of empty is used
 */
class MimeTypeOptions(val type: String, val subtype: String, val parameters: List<String>) : CspDirectiveOptions {
    override val asString: String = "$type/$subtype${parameters.joinToString { ";$it" }}"
    companion object : OptionParser<MimeTypeOptions> {
        override fun parseFrom(string: String): List<MimeTypeOptions>? =
            string.split(' ').mapNotNull { type ->
                type.split(';').let { parts ->
                    parts.firstOrNull()?.split('/')?.let { typeAndSubtype ->
                        kotlin.runCatching {
                            MimeTypeOptions(typeAndSubtype[0], typeAndSubtype[1], parts.subList(1, parts.size))
                        }.getOrNull()
                    }
                }
            }
    }
}