package directives

import directives.options.*

/**
 * Superclass over all directive objects. Some things (like a name) that are necessary are instead present on
 * the parsing companion object interface, [DirectiveParser].
 *
 * Note that not every single possible directive is enumerated here. Specifically, thoroughly deprecated directives
 * where usage is discouraged are absent here. The point of this module is to help build modern and effective CSPs, not
 * handle deprecated or obsolete cruft.
 * Omitted due to deprecation:
 * - https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/referrer
 * - https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/require-sri-for
 * Omitted due to experimental state (no browser support, documentation, or examples)
 * - https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/trusted-types
 */
sealed class CspDirective<Options : CspDirectiveOptions>(val options: Collection<Options>) {
    val optionsAsString: String = options.joinToString(separator = " ") { it.asString }
    open fun adjustToUri(uri: String, selfPattern: Regex): CspDirective<Options>? = null

    companion object {
        /**
         * A simple list of all directives. This can be used with impunity since everything here is actually a static
         * builder.
         */
        val directives: Collection<DirectiveParser<out CspDirectiveOptions, out CspDirective<*>>> = buildList {
            addAll(DocumentDirective.directives)
            addAll(FetchDirective.directives)
            addAll(NavigationDirective.directives)
            addAll(ReportingDirective.directives)
            add(BlockAllMixedContent)
            add(UpgradeInsecureRequests)
        }
    }

    /**
     * Directives that control the properties of the entire document
     *
     * https://developer.mozilla.org/en-US/docs/Glossary/Document_directive
     */
    sealed class DocumentDirective<Options : CspDirectiveOptions>(options: Collection<Options>) :
        CspDirective<Options>(options) {
        companion object {
            val directives: Collection<DirectiveParser<out CspDirectiveOptions, out DocumentDirective<*>>> = listOf(
                BaseUri, PluginTypes, Sandbox
            )
        }

        /**
         * Restrict URLs in base elements. Uses [SourceOptions]s, though some don't make logical sense for the
         * element.
         *
         * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/base-uri
         */
        class BaseUri(options: Collection<SourceOptions>) : DocumentDirective<SourceOptions>(options) {
            override fun adjustToUri(uri: String, selfPattern: Regex): CspDirective<SourceOptions>? =
                BaseUri(SourceOptions.adjustToUri(options, uri, selfPattern))

            companion object :
                DirectiveParser<SourceOptions, BaseUri> {
                override val name: String = "base-uri"
                override val optionParser: OptionParser<SourceOptions> =
                    SourceOptions
                override val directiveConstructor: (List<SourceOptions>) -> BaseUri = ::BaseUri
            }
        }

        /**
         * Restricts the MIME types that can be loaded via embed, object, and applet elements. Only really has an effect
         * if [FetchDirective.ObjectSrc] is not 'none'. See [MimeTypeOptions] for a bit more information on how this
         * works.
         *
         * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/plugin-types
         */
        class PluginTypes(options: Collection<MimeTypeOptions>) : DocumentDirective<MimeTypeOptions>(options) {
            companion object : DirectiveParser<MimeTypeOptions, PluginTypes> {
                override val name: String = "plugin-types"
                override val optionParser: OptionParser<MimeTypeOptions> = MimeTypeOptions
                override val directiveConstructor: (List<MimeTypeOptions>) -> PluginTypes = ::PluginTypes
            }
        }

        /**
         * Enables a sandbox similar to an iframe's sandbox attribute.
         *
         * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/sandbox
         */
        class Sandbox(options: Collection<SandboxOptions>) : DocumentDirective<SandboxOptions>(options) {
            companion object : DirectiveParser<SandboxOptions, Sandbox> {
                override val name: String = "sandbox"
                override val optionParser: OptionParser<SandboxOptions> = SandboxOptions
                override val directiveConstructor: (List<SandboxOptions>) -> Sandbox = ::Sandbox
                override val canBeInMetaElement: Boolean = false
                override val canBeInReportOnlyHeader: Boolean = false
            }
        }
    }

    /**
     * Directives that control where resources can be loaded from
     *
     * These generally fallback to [DefaultSrc]
     *
     * https://developer.mozilla.org/en-US/docs/Glossary/Fetch_directive
     */
    sealed class FetchDirective<Options : CspDirectiveOptions>(options: Collection<Options>) :
        CspDirective<Options>(options) {

        companion object {
            val directives: Collection<DirectiveParser<out CspDirectiveOptions, out FetchDirective<*>>> = listOf(
                ChildSrc,
                ConnectSrc,
                DefaultSrc,
                FontSrc,
                FrameSrc,
                ImgSrc,
                ManifestSrc,
                MediaSrc,
                ObjectSrc,
                PrefetchSrc,
                ScriptSrc,
                ScriptSrcAttr,
                ScriptSrcElem,
                StyleSrc,
                StyleSrcAttr,
                StyleSrcElem,
                WorkerSrc
            )
        }

        /**
         * Defines sources for child elements like frame and iframe and web workers
         *
         * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/child-src
         */
        class ChildSrc(options: Collection<SourceOptions>) : FetchDirective<SourceOptions>(options) {
            override fun adjustToUri(uri: String, selfPattern: Regex): CspDirective<SourceOptions>? =
                ChildSrc(SourceOptions.adjustToUri(options, uri, selfPattern))
            companion object :
                DirectiveParser<SourceOptions, ChildSrc> {
                override val name: String = "child-src"
                override val optionParser: OptionParser<SourceOptions> =
                    SourceOptions
                override val directiveConstructor: (List<SourceOptions>) -> ChildSrc = ::ChildSrc
            }
        }

        /**
         * Defines URLs that can be loaded via scripts
         *
         * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/connect-src
         */
        class ConnectSrc(options: Collection<SourceOptions>) : FetchDirective<SourceOptions>(options) {
            override fun adjustToUri(uri: String, selfPattern: Regex): CspDirective<SourceOptions>? =
                ConnectSrc(SourceOptions.adjustToUri(options, uri, selfPattern))
            companion object :
                DirectiveParser<SourceOptions, ConnectSrc> {
                override val name: String = "connect-src"
                override val optionParser: OptionParser<SourceOptions> =
                    SourceOptions
                override val directiveConstructor: (List<SourceOptions>) -> ConnectSrc = ::ConnectSrc
            }
        }

        /**
         * Defines a fallback for other [FetchDirective]s
         *
         * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/default-src
         */
        class DefaultSrc(options: Collection<SourceOptions>) : FetchDirective<SourceOptions>(options) {
            override fun adjustToUri(uri: String, selfPattern: Regex): CspDirective<SourceOptions>? =
                DefaultSrc(SourceOptions.adjustToUri(options, uri, selfPattern))
            companion object :
                DirectiveParser<SourceOptions, DefaultSrc> {
                override val name: String = "default-src"
                override val optionParser: OptionParser<SourceOptions> =
                    SourceOptions
                override val directiveConstructor: (List<SourceOptions>) -> DefaultSrc = ::DefaultSrc
            }
        }

        /**
         * Defines sources for fonts loaded via "@font-face"
         *
         * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/font-src
         */
        class FontSrc(options: Collection<SourceOptions>) : FetchDirective<SourceOptions>(options) {
            override fun adjustToUri(uri: String, selfPattern: Regex): CspDirective<SourceOptions>? =
                FontSrc(SourceOptions.adjustToUri(options, uri, selfPattern))
            companion object :
                DirectiveParser<SourceOptions, FontSrc> {
                override val name: String = "font-src"
                override val optionParser: OptionParser<SourceOptions> =
                    SourceOptions
                override val directiveConstructor: (List<SourceOptions>) -> FontSrc = ::FontSrc
            }
        }

        /**
         * Defines sources for nested browsing loaded via things like frame and iframe
         *
         * Falls back to [ChildSrc] first before [DefaultSrc]
         *
         * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/frame-src
         */
        class FrameSrc(options: Collection<SourceOptions>) : FetchDirective<SourceOptions>(options) {
            override fun adjustToUri(uri: String, selfPattern: Regex): CspDirective<SourceOptions>? =
                FrameSrc(SourceOptions.adjustToUri(options, uri, selfPattern))
            companion object :
                DirectiveParser<SourceOptions, FrameSrc> {
                override val name: String = "frame-src"
                override val optionParser: OptionParser<SourceOptions> =
                    SourceOptions
                override val directiveConstructor: (List<SourceOptions>) -> FrameSrc = ::FrameSrc
            }
        }

        /**
         * Defines sources for images and favicons
         *
         * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/img-src
         */
        class ImgSrc(options: Collection<SourceOptions>) : FetchDirective<SourceOptions>(options) {
            override fun adjustToUri(uri: String, selfPattern: Regex): CspDirective<SourceOptions>? =
                ImgSrc(SourceOptions.adjustToUri(options, uri, selfPattern))
            companion object :
                DirectiveParser<SourceOptions, ImgSrc> {
                override val name: String = "img-src"
                override val optionParser: OptionParser<SourceOptions> =
                    SourceOptions
                override val directiveConstructor: (List<SourceOptions>) -> ImgSrc = ::ImgSrc
            }
        }

        /**
         * Defines where PWA manifests can be sourced from
         *
         * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/manifest-src
         */
        class ManifestSrc(options: Collection<SourceOptions>) : FetchDirective<SourceOptions>(options) {
            override fun adjustToUri(uri: String, selfPattern: Regex): CspDirective<SourceOptions>? =
                ManifestSrc(SourceOptions.adjustToUri(options, uri, selfPattern))
            companion object :
                DirectiveParser<SourceOptions, ManifestSrc> {
                override val name: String = "manifest-src"
                override val optionParser: OptionParser<SourceOptions> =
                    SourceOptions
                override val directiveConstructor: (List<SourceOptions>) -> ManifestSrc = ::ManifestSrc
            }
        }

        /**
         * Defines where media via audio and video elements can come from
         *
         * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/media-src
         */
        class MediaSrc(options: Collection<SourceOptions>) : FetchDirective<SourceOptions>(options) {
            override fun adjustToUri(uri: String, selfPattern: Regex): CspDirective<SourceOptions>? =
                MediaSrc(SourceOptions.adjustToUri(options, uri, selfPattern))
            companion object :
                DirectiveParser<SourceOptions, MediaSrc> {
                override val name: String = "media-src"
                override val optionParser: OptionParser<SourceOptions> =
                    SourceOptions
                override val directiveConstructor: (List<SourceOptions>) -> MediaSrc = ::MediaSrc
            }
        }

        /**
         * Defines where objects loaded via object, embed, or applet elements can come from
         *
         * Types should not be defines here, those are on [PluginTypes]
         *
         * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/object-src
         */
        class ObjectSrc(options: Collection<SourceOptions>) : FetchDirective<SourceOptions>(options) {
            override fun adjustToUri(uri: String, selfPattern: Regex): CspDirective<SourceOptions>? =
                ObjectSrc(SourceOptions.adjustToUri(options, uri, selfPattern))
            companion object :
                DirectiveParser<SourceOptions, ObjectSrc> {
                override val name: String = "object-src"
                override val optionParser: OptionParser<SourceOptions> =
                    SourceOptions
                override val directiveConstructor: (List<SourceOptions>) -> ObjectSrc = ::ObjectSrc
            }
        }

        /**
         * Defines what resources can be prefetched or prerendered
         *
         * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/prefetch-src
         */
        class PrefetchSrc(options: Collection<SourceOptions>) : FetchDirective<SourceOptions>(options) {
            override fun adjustToUri(uri: String, selfPattern: Regex): CspDirective<SourceOptions>? =
                PrefetchSrc(SourceOptions.adjustToUri(options, uri, selfPattern))
            companion object :
                DirectiveParser<SourceOptions, PrefetchSrc> {
                override val name: String = "prefetch-src"
                override val optionParser: OptionParser<SourceOptions> =
                    SourceOptions
                override val directiveConstructor: (List<SourceOptions>) -> PrefetchSrc = ::PrefetchSrc
            }
        }

        /**
         * Defines where JavaScript can be loaded from
         *
         * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/script-src
         */
        class ScriptSrc(options: Collection<SourceOptions>) : FetchDirective<SourceOptions>(options) {
            override fun adjustToUri(uri: String, selfPattern: Regex): CspDirective<SourceOptions>? =
                ScriptSrc(SourceOptions.adjustToUri(options, uri, selfPattern))
            companion object :
                DirectiveParser<SourceOptions, ScriptSrc> {
                override val name: String = "script-src"
                override val optionParser: OptionParser<SourceOptions> =
                    SourceOptions
                override val directiveConstructor: (List<SourceOptions>) -> ScriptSrc = ::ScriptSrc
            }
        }

        /**
         * Defines where JavaScript can be loaded from just for inline event handlers
         *
         * Falls back to [ScriptSrc] before [DefaultSrc]
         *
         * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/script-src-attr
         */
        class ScriptSrcAttr(options: Collection<SourceOptions>) : FetchDirective<SourceOptions>(options) {
            override fun adjustToUri(uri: String, selfPattern: Regex): CspDirective<SourceOptions>? =
                ScriptSrcAttr(SourceOptions.adjustToUri(options, uri, selfPattern))
            companion object :
                DirectiveParser<SourceOptions, ScriptSrcAttr> {
                override val name: String = "script-src-attr"
                override val optionParser: OptionParser<SourceOptions> =
                    SourceOptions
                override val directiveConstructor: (List<SourceOptions>) -> ScriptSrcAttr = ::ScriptSrcAttr
            }
        }

        /**
         * Defines where JavaScript can be loaded from just for inline script elements
         *
         * Falls back to [ScriptSrc] before [DefaultSrc]
         *
         * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/script-src-elem
         */
        class ScriptSrcElem(options: Collection<SourceOptions>) : FetchDirective<SourceOptions>(options) {
            override fun adjustToUri(uri: String, selfPattern: Regex): CspDirective<SourceOptions>? =
                ScriptSrcElem(SourceOptions.adjustToUri(options, uri, selfPattern))
            companion object :
                DirectiveParser<SourceOptions, ScriptSrcElem> {
                override val name: String = "script-src-elem"
                override val optionParser: OptionParser<SourceOptions> =
                    SourceOptions
                override val directiveConstructor: (List<SourceOptions>) -> ScriptSrcElem = ::ScriptSrcElem
            }
        }

        /**
         * Defines where Styles can be loaded from
         *
         * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/style-src
         */
        class StyleSrc(options: Collection<SourceOptions>) : FetchDirective<SourceOptions>(options) {
            override fun adjustToUri(uri: String, selfPattern: Regex): CspDirective<SourceOptions>? =
                StyleSrc(SourceOptions.adjustToUri(options, uri, selfPattern))
            companion object :
                DirectiveParser<SourceOptions, StyleSrc> {
                override val name: String = "style-src"
                override val optionParser: OptionParser<SourceOptions> =
                    SourceOptions
                override val directiveConstructor: (List<SourceOptions>) -> StyleSrc = ::StyleSrc
            }
        }

        /**
         * Defines where Styles can be loaded from just for inline styles for individual elements
         *
         * Falls back to [StyleSrc] before [DefaultSrc]
         *
         * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/style-src-attr
         */
        class StyleSrcAttr(options: Collection<SourceOptions>) : FetchDirective<SourceOptions>(options) {
            override fun adjustToUri(uri: String, selfPattern: Regex): CspDirective<SourceOptions>? =
                StyleSrcAttr(SourceOptions.adjustToUri(options, uri, selfPattern))
            companion object :
                DirectiveParser<SourceOptions, StyleSrcAttr> {
                override val name: String = "style-src-attr"
                override val optionParser: OptionParser<SourceOptions> =
                    SourceOptions
                override val directiveConstructor: (List<SourceOptions>) -> StyleSrcAttr = ::StyleSrcAttr
            }
        }

        /**
         * Defines where Styles can be loaded from just for inline style elements and link elements with
         * rel="stylesheet"
         *
         * Falls back to [StyleSrc] before [DefaultSrc]
         *
         * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/style-src-elem
         */
        class StyleSrcElem(options: Collection<SourceOptions>) : FetchDirective<SourceOptions>(options) {
            override fun adjustToUri(uri: String, selfPattern: Regex): CspDirective<SourceOptions>? =
                StyleSrcElem(SourceOptions.adjustToUri(options, uri, selfPattern))
            companion object :
                DirectiveParser<SourceOptions, StyleSrcElem> {
                override val name: String = "style-src-elem"
                override val optionParser: OptionParser<SourceOptions> =
                    SourceOptions
                override val directiveConstructor: (List<SourceOptions>) -> StyleSrcElem = ::StyleSrcElem
            }
        }

        /**
         * Defines sources for different kinds of worker scripts
         *
         * Technically should fall back to [ChildSrc], then [ScriptSrc], then [DefaultSrc], but this is not consistent
         * across browsers
         *
         * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/worker-src
         */
        class WorkerSrc(options: Collection<SourceOptions>) : FetchDirective<SourceOptions>(options) {
            override fun adjustToUri(uri: String, selfPattern: Regex): CspDirective<SourceOptions>? =
                WorkerSrc(SourceOptions.adjustToUri(options, uri, selfPattern))
            companion object :
                DirectiveParser<SourceOptions, WorkerSrc> {
                override val name: String = "worker-src"
                override val optionParser: OptionParser<SourceOptions> =
                    SourceOptions
                override val directiveConstructor: (List<SourceOptions>) -> WorkerSrc = ::WorkerSrc
            }
        }
    }

    /**
     * Directives that control where a user can navigate to
     *
     * https://developer.mozilla.org/en-US/docs/Glossary/Navigation_directive
     */
    sealed class NavigationDirective<Options : CspDirectiveOptions>(options: Collection<Options>) :
        CspDirective<Options>(options) {

        companion object {
            val directives: Collection<DirectiveParser<out CspDirectiveOptions, out NavigationDirective<*>>> = listOf(
                FormAction, FrameAncestors, NavigateTo
            )
        }

        /**
         * Defines what URLs can be used to target form submissions
         *
         * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/form-action
         */
        class FormAction(options: Collection<SourceOptions>) : NavigationDirective<SourceOptions>(options) {
            override fun adjustToUri(uri: String, selfPattern: Regex): CspDirective<SourceOptions>? =
                FormAction(SourceOptions.adjustToUri(options, uri, selfPattern))
            companion object :
                DirectiveParser<SourceOptions, FormAction> {
                override val name: String = "form-action"
                override val optionParser: OptionParser<SourceOptions> =
                    SourceOptions
                override val directiveConstructor: (List<SourceOptions>) -> FormAction = ::FormAction
            }
        }

        /**
         * Defines parents that a page can embed using things like frame or embed
         *
         * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/frame-ancestors
         */
        class FrameAncestors(options: Collection<SourceOptions>) : NavigationDirective<SourceOptions>(options) {
            override fun adjustToUri(uri: String, selfPattern: Regex): CspDirective<SourceOptions>? =
                FrameAncestors(SourceOptions.adjustToUri(options, uri, selfPattern))
            companion object :
                DirectiveParser<SourceOptions, FrameAncestors> {
                override val name: String = "frame-ancestors"
                override val optionParser: OptionParser<SourceOptions> =
                    SourceOptions
                override val directiveConstructor: (List<SourceOptions>) -> FrameAncestors = ::FrameAncestors
                override val canBeInMetaElement: Boolean = false
            }
        }

        /**
         * Defines where the document can initiate navigations (not where the document is allowed to navigate to, just
         * where it can initiate)
         *
         * Does not override [FormAction] for form submissions
         *
         * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/navigate-to
         */
        class NavigateTo(options: Collection<SourceOptions>) : NavigationDirective<SourceOptions>(options) {
            override fun adjustToUri(uri: String, selfPattern: Regex): CspDirective<SourceOptions>? =
                NavigateTo(SourceOptions.adjustToUri(options, uri, selfPattern))
            companion object :
                DirectiveParser<SourceOptions, NavigateTo> {
                override val name: String = "navigate-to"
                override val optionParser: OptionParser<SourceOptions> =
                    SourceOptions
                override val directiveConstructor: (List<SourceOptions>) -> NavigateTo = ::NavigateTo
                override val canBeInMetaElement: Boolean = false
            }
        }
    }

    /**
     * Directives that control violation reports
     *
     * https://developer.mozilla.org/en-US/docs/Glossary/Reporting_directive
     */
    sealed class ReportingDirective<Options : CspDirectiveOptions>(options: Collection<Options>) :
        CspDirective<Options>(options) {

        companion object {
            val directives: Collection<DirectiveParser<out CspDirectiveOptions, out ReportingDirective<*>>> = listOf(
                ReportTo, ReportUri
            )
        }

        /**
         * Directs user agent to send reports to the given group name, specified in a Report-To HTTP header
         *
         * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/report-to
         */
        class ReportTo(options: Collection<ArbitraryTextOptions>) : ReportingDirective<ArbitraryTextOptions>(options) {
            companion object : DirectiveParser<ArbitraryTextOptions, ReportTo> {
                override val name: String = "report-to"
                override val optionParser: OptionParser<ArbitraryTextOptions> = ArbitraryTextOptions
                override val directiveConstructor: (List<ArbitraryTextOptions>) -> ReportTo = ::ReportTo
                override val canBeInMetaElement: Boolean = false
                fun constructSimple(group: String) = ReportTo(listOf(ArbitraryTextOptions(group)))
            }
        }

        /**
         * URI to post violation reports to. Parsing of the URIs here is as permissible as possible (more permissible
         * than what a valid URI would be, since that sort of validation isn't the point here)
         *
         * This is deprecated but still used because [ReportTo] isn't implemented everywhere
         *
         * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/report-uri
         */
        class ReportUri(options: Collection<ArbitraryTextOptions>) : ReportingDirective<ArbitraryTextOptions>(options) {
            companion object : DirectiveParser<ArbitraryTextOptions, ReportUri> {
                override val name: String = "report-uri"
                override val optionParser: OptionParser<ArbitraryTextOptions> = ArbitraryTextOptions
                override val directiveConstructor: (List<ArbitraryTextOptions>) -> ReportUri = ::ReportUri
                override val canBeInMetaElement: Boolean = false
                fun constructSimple(uri: String) = ReportUri(listOf(ArbitraryTextOptions(uri)))
            }
        }
    }

    /**
     * Block HTTP when page is loaded via HTTPS
     *
     * Overridden by [UpgradeInsecureRequests]
     *
     * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/block-all-mixed-content
     */
    class BlockAllMixedContent : CspDirective<NoOptions>(listOf()) {
        companion object :
            DirectiveParser<NoOptions, BlockAllMixedContent> {
            override val name: String = "block-all-mixed-content"
            override val optionParser: OptionParser<NoOptions> =
                NoOptions
            override val directiveConstructor: (List<NoOptions>) -> BlockAllMixedContent = { BlockAllMixedContent() }
        }
    }

    /**
     * Force all resources ot be loaded over HTTPS
     *
     * Overrides [BlockAllMixedContent]
     *
     * https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/upgrade-insecure-requests
     */
    class UpgradeInsecureRequests : CspDirective<NoOptions>(listOf()) {
        companion object :
            DirectiveParser<NoOptions, UpgradeInsecureRequests> {
            override val name: String = "upgrade-insecure-requests"
            override val optionParser: OptionParser<NoOptions> =
                NoOptions
            override val directiveConstructor: (List<NoOptions>) -> UpgradeInsecureRequests =
                { UpgradeInsecureRequests() }
        }
    }
}