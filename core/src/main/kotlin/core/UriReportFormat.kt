package core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UriReportFormat(@SerialName("csp-report") val data: CspReport?) {
    @Serializable
    data class CspReport(
        @SerialName("blocked-uri") val blockedUri: String? = null,
        val disposition: String? = null ,
        @SerialName("document-uri") val documentUri: String? = null,
        @SerialName("effective-directive") val effectiveDirective: String? = null,
        @SerialName("original-policy") val originalPolicy: String? = null,
        val referrer: String? = null,
        @SerialName("script-sample") val scriptSample: String? = null,
        @SerialName("status-code") val statusCode: String? = null,
        @SerialName("violated-directive") val violatedDirective: String? = null
    )
}