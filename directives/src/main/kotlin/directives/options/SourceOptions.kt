package directives.options

/**
 * Options describing a source, usually used for various [directives.FetchDirective]s
 *
 * An example would be [directives.DefaultSrc]'s directives.options:
 * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/default-src
 */
sealed class SourceOptions : CspDirectiveOptions {
    companion object : MultiOptionParser<SourceOptions> {
        /**
         * The order of these directives.options is just so that more narrow one are parsed first, to avoid any issues due to more
         * permissive regular expressions than the specification
         */
        override val possibleOptions: List<SingleOptionParser<out SourceOptions>> = listOf(
            Self,
            UnsafeEval,
            UnsafeHashes,
            UnsafeInline,
            None,
            StrictDynamic,
            ReportSample,
            Nonce,
            Hash,
            SchemeSource,
            HostSource
        )

        override val strictest: List<SourceOptions>
            get() = listOf(None)

        fun adjustToUri(existing: Collection<SourceOptions>, uri: String, selfPattern: Regex): Collection<SourceOptions> {
            when {
                selfPattern.matches(uri) -> listOf(Self)
                uri == "inline" -> listOf(UnsafeInline)
                uri == "eval" -> listOf(UnsafeEval)
                uri.matches(Regex("[a-zA-Z\\-]+")) -> listOf(SchemeSource(scheme = uri))
                else -> HostSource.pattern.find(uri)?.groupValues?.get(0)?.let(HostSource.Companion::parseFrom)
            }.run {
                return if (this != null)
                    existing.toMutableSet().also {
                        it.addAll(this)
                        it.remove(None)
                    }
                else {
                    println("Couldn't handle URI: $uri")
                    existing
                }
            }
        }
    }

    /**
     * An internet [host] with possibly some URL [scheme] and/or [port] number
     */
    data class HostSource(val host: String, val scheme: String? = null, val port: Int? = null) : SourceOptions() {
        override val asString: String = "${scheme?.let { "$it://" }}$host${port?.let { ":$it" } ?: ""}"

        companion object : SingleOptionParser<HostSource> {
            override val pattern = Regex("(?:(\\w+):/?/?)?([\\w\\-~.]+)(?::(\\d+))?")
            override fun constructFrom(matcher: MatchResult) = HostSource(
                host = matcher.groupValues[2],
                scheme = matcher.groupValues[1],
                port = matcher.groupValues[3].toIntOrNull()
            )
        }
    }

    /**
     * Some url [scheme]
     */
    data class SchemeSource(val scheme: String) : SourceOptions() {
        override val asString: String = "$scheme:"

        companion object : SingleOptionParser<SchemeSource> {
            override val pattern = Regex("(?:(\\w+):)")
            override fun constructFrom(matcher: MatchResult) =
                SchemeSource(matcher.groupValues[1])
        }
    }

    /**
     * Refers to the origin the document was served from (exact scheme and port number too)
     */
    object Self : SourceOptions(), SingleOptionParser<Self> {
        override val asString: String = "'self'"
        override val pattern = Regex(asString)
        override fun constructFrom(matcher: MatchResult) = Self
    }

    /**
     * Allows `eval()` and similar methods
     */
    object UnsafeEval : SourceOptions(), SingleOptionParser<UnsafeEval> {
        override val asString: String = "'unsafe-eval'"
        override val pattern = Regex(asString)
        override fun constructFrom(matcher: MatchResult) = UnsafeEval
    }

    /**
     * Allows inline event handlers specified by hash directives.options
     */
    object UnsafeHashes : SourceOptions(), SingleOptionParser<UnsafeHashes> {
        override val asString: String = "'unsafe-hashes'"
        override val pattern = Regex(asString)
        override fun constructFrom(matcher: MatchResult) = UnsafeHashes
    }

    /**
     * Allows inline resources like <script>, <style>, inline event handlers, and the 'javascript:' scheme
     */
    object UnsafeInline : SourceOptions(), SingleOptionParser<UnsafeInline> {
        override val asString: String = "'unsafe-inline'"
        override val pattern = Regex(asString)
        override fun constructFrom(matcher: MatchResult) = UnsafeInline
    }

    /**
     * Matches nothing
     */
    object None : SourceOptions(), SingleOptionParser<None> {
        override val asString: String = "'none'"
        override val pattern = Regex(asString)
        override fun constructFrom(matcher: MatchResult) = None
    }

    /**
     * Allow inline scripts via a [nonce], overriding [UnsafeInline] in supported browsers
     */
    data class Nonce(val nonce: String) : SourceOptions() {
        override val asString: String = "'nonce-$nonce'"

        companion object : SingleOptionParser<Nonce> {
            override val pattern = Regex("'nonce-([^'-]+)'")
            override fun constructFrom(matcher: MatchResult) = Nonce(
                nonce = matcher.groupValues[1]
            )
        }
    }

    /**
     * Allow scripts or styles by the [hash] of the content via some [algorithm]
     */
    data class Hash(val algorithm: String, val hash: String) : SourceOptions() {
        override val asString = "'$algorithm-$hash'"

        companion object : SingleOptionParser<Hash> {
            override val pattern = Regex("'([^'-]+)-([^'-]+)'")
            override fun constructFrom(matcher: MatchResult) = Hash(
                algorithm = matcher.groupValues[1],
                hash = matcher.groupValues[2]
            )
        }
    }

    /**
     * Propagates trust through scripts allowed via [Nonce] or [Hash] and disables whitelists like [Self],
     * [UnsafeInline], and [HostSource]
     */
    object StrictDynamic : SourceOptions(), SingleOptionParser<StrictDynamic> {
        override val asString: String = "'strict-dynamic'"
        override val pattern = Regex(asString)
        override fun constructFrom(matcher: MatchResult) = StrictDynamic
    }

    /**
     * A sample of the violating code will be included in violation reports
     */
    object ReportSample : SourceOptions(), SingleOptionParser<ReportSample> {
        override val asString: String = "'report-sample'"
        override val pattern = Regex(asString)
        override fun constructFrom(matcher: MatchResult) = ReportSample
    }


}