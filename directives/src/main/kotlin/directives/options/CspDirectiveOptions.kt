package directives.options

/**
 * Interface over all directives.options possible for various [directives.CspDirective]s.
 *
 * The general pattern for directives.options is that each instance represents one option and the class itself has static
 * methods to parse a bunch of itself from a string.
 */
interface CspDirectiveOptions {
    val asString: String
}