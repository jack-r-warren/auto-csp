package core

import directives.CspDirective

class PolicyBuilder (policyGetter: () -> Map<String, CspDirective<*>>, val selfPattern: Regex) {
    val policy =  policyGetter.invoke().toMutableMap()

    fun evaluateViolation(data: UriReportFormat.CspReport) {
        data.effectiveDirective?.let { directiveName ->
            policy.computeIfPresent(directiveName) { _, directive ->
                when {
                    data.blockedUri != null -> directive.adjustToUri(data.blockedUri, selfPattern)
                    else -> {
                        println("Unhandled violation type: $data")
                        null
                    }
                }
            }
        }
    }
}