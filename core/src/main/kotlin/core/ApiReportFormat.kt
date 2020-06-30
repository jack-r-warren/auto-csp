package core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiReportFormat(
    val type: String? = null,
    val age: Int? = null,
    val url: String? = null,
    @SerialName("user_agent") val userAgent: String? = null,
    val body: Body? = null
) {
    @Serializable
    data class Body(
        val blocked: String? = null,
        val directive: String? = null,
        val policy: String? = null,
        val status: String? = null,
        val referrer: String? = null
    )
}