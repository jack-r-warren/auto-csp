package directives.options

/**
 * Options specific to the [directives.CspDirective.DocumentDirective.Sandbox] directives.
 *
 * "Strict" in this context means nothing specifically allowed, so that value on [OptionParser] is left as-is.
 *
 * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/sandbox
 */
class SandboxOptions(val allow: String) : CspDirectiveOptions {
    override val asString: String = allow

    companion object : OptionParser<SandboxOptions> {
        private val possibleValues = setOf(
            "allow-downloads-without-user-activation",
            "allow-forms",
            "allow-modals",
            "allow-orientation-lock",
            "allow-pointer-lock",
            "allow-popups",
            "allow-popups-to-escape-sandbox",
            "allow-presentation",
            "allow-same-origin",
            "allow-scripts",
            "allow-storage-access-by-user-activation",
            "allow-top-navigation",
            "allow-top-navigation-by-user-activation"
        )

        override fun parseFrom(string: String): List<SandboxOptions>? =
            string.split(' ').filter { it in possibleValues }.map(::SandboxOptions)
    }
}