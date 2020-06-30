package core

import directives.util.makeStrictPolicy
import directives.util.toPolicyString
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.client.HttpClient
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.charset
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.request.uri
import io.ktor.response.respond
import io.ktor.server.engine.embeddedServer
import io.ktor.server.jetty.Jetty
import io.ktor.util.cio.use
import io.ktor.util.cio.write
import io.ktor.util.filter
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

open class Proxy(
    val reportingApiEndpoint: String? = null,
    val reportUriEndpoint: String? = null,
    val proxyPort: Int,
    val targetDomain: String
) {
    var addUrls: (suspend (Sequence<String>) -> Unit)? = null
    var manualParsing: Map<Regex, suspend (Sequence<MatchResult>) -> Unit> = emptyMap()
    val reportingApiGroup: String? =
        reportingApiEndpoint?.let {
            """
            {
                "group": "csp-endpoint",
                "max_age": 10886400,
                "endpoints": [{
                    "url": "$it"
                }]
            }
        """.trimIndent().replace('\n', ' ')
        }

    val policyHeader = makeStrictPolicy(
        reportUriEndpoint = reportUriEndpoint,
        reportingApiGroup = reportingApiGroup?.let { "csp-endpoint" }
    ).toPolicyString()

    open fun run(): Unit = runBlocking {
        launch {
            proxyServer.start(wait = true)
        }
    }

    val replacePattern = Regex("(https?:)?//${targetDomain.replace(".", "\\.")}")
    private fun String.replaceDomain() = replace(replacePattern, "")

    val browsePatterns = listOf(
        Regex("(?:href|action)=\"(?:https?://)?(?:${targetDomain.replace(".", "\\.")})?([^.\"#?]+(?:html?)?)\"")
    )

    private suspend fun String.proxyParse() = apply {
        if (addUrls != null) browsePatterns.forEach { pattern ->
            addUrls?.invoke(pattern.findAll(this)
                .map { it.groupValues[1] }
                .filter { it.isNotBlank() })
        }
        manualParsing.forEach { (pattern, function) ->
            function.invoke(pattern.findAll(this))
        }
    }

    protected val proxyServer by lazy {
        embeddedServer(
            Jetty,
            port = proxyPort
        ) {
            val client = HttpClient()
            intercept(ApplicationCallPipeline.Call) {
                val proxiedResponse =
                    client.request<HttpResponse>("https://${targetDomain}${call.request.uri}")


                object : OutgoingContent.WriteChannelContent() {
                    override val status = proxiedResponse.status
                    override val contentType = proxiedResponse.contentType()
                    override val headers = Headers.build {
                        with(proxiedResponse.headers) {
                            get(HttpHeaders.Location)?.let {
                                append(
                                    HttpHeaders.Location,
                                    it.replaceDomain()
                                )
                            }
                            reportingApiGroup?.let { append("Report-To", it) }
                            append("Content-Security-Policy-Report-Only", policyHeader)
                            appendMissing(filter { name, _ ->
                                !HttpHeaders.isUnsafe(name) && !name.equals(
                                    "content-security-policy",
                                    ignoreCase = true
                                )
                            })
                        }
                    }

                    override suspend fun writeTo(channel: ByteWriteChannel) {
                        when {
                            contentType?.let {
                                (it.contentType == "text" && it.contentSubtype == "html")
                            } == true ->
                                channel.use {
                                    write(
                                        proxiedResponse.readText().proxyParse().replaceDomain(),
                                        proxiedResponse.charset() ?: Charsets.UTF_8
                                    )
                                }
                            else -> proxiedResponse.content.copyAndClose(channel)
                        }
                    }
                }.let { call.respond(it) }
            }
        }
    }
}