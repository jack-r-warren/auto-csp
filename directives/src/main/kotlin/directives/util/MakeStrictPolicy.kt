package directives.util

import directives.CspDirective

fun makeStrictPolicy(
    reportingApiGroup: String? = null,
    reportUriEndpoint: String? = null
): Map<String, CspDirective<*>> = CspDirective.directives.filter {
    it.canBeInReportOnlyHeader
}.associateBy {
    it.name
}.mapNotNull { (key, value) ->
    when (value) {
        is CspDirective.ReportingDirective.ReportUri.Companion ->
            reportUriEndpoint?.let { value.constructSimple(it) }
        is CspDirective.ReportingDirective.ReportTo.Companion ->
            reportingApiGroup?.let { value.constructSimple(it) }
        else ->
            value.makeStrict()
    }?.let { key to it }
}.toMap()