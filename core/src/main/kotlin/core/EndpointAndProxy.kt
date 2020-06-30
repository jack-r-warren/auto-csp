package core

import directives.util.makeStrictPolicy
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.serialization.DefaultJsonConfiguration
import io.ktor.serialization.json
import io.ktor.server.engine.embeddedServer
import io.ktor.server.jetty.Jetty
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

open class EndpointAndProxy(
    endpointPort: Int,
    proxyPort: Int,
    targetDomain: String,
) :
    Proxy(
        reportUriEndpoint = "http://localhost:$endpointPort/uri",
        //reportingApiEndpoint = "http://localhost:$endpointPort/api",
        proxyPort = proxyPort,
        targetDomain = targetDomain
    ) {

    val selfRegex: Regex = Regex("(https?://localhost:$proxyPort).*")
    val policyBuilder: PolicyBuilder =
        PolicyBuilder(::makeStrictPolicy, selfRegex)

    override fun run(): Unit = runBlocking {
        launch { endpointServer.start(wait = false) }
        launch { proxyServer.start(wait = false) }
    }

    val endpointServer =
        embeddedServer(
            Jetty,
            port = endpointPort
        ) {
            install(CORS) {
                anyHost()
                method(HttpMethod.Get)
                method(HttpMethod.Put)
                method(HttpMethod.Post)
                method(HttpMethod.Delete)
                method(HttpMethod.Options)
                header("Content-Type")
                header("Authorization")
                header("Content-Length")
                header("X-Requested-Width")
            }
            install(ContentNegotiation) {
                json(
                    contentType = ContentType.parse("application/csp-report"),
                    json = Json(
                        DefaultJsonConfiguration.copy(
                            ignoreUnknownKeys = true
                        )
                    )
                )
                json(
                    contentType = ContentType.parse("application/reports+json"),
                    json = Json(
                        DefaultJsonConfiguration.copy(
                            ignoreUnknownKeys = true
                        )
                    )
                )
            }
            routing {
                post("/uri") {
                    with(call.receive<UriReportFormat>()) {
                        call.respond(200)
                        data?.let(policyBuilder::evaluateViolation)
                    }
                }
                post("/api") {
                    with(call.receive<ApiReportFormat>()) {
                        if (type == "csp-violation") body?.let { println(it) }
                    }
                }
            }
        }
}