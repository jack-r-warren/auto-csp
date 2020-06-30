package core

import directives.util.toPolicyString
import io.ktor.server.engine.stop
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.actor
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.time.minutes
import kotlin.time.seconds

class AutomatedBrowser(
    endpointPort: Int,
    proxyPort: Int,
    targetDomain: String,
    private val startingUris: Collection<String>,
    private val browser: Browser,
    private val loadDelaySeconds: Int,
    private val timeoutMinutes: Int,
    private val logFile: File?
) : EndpointAndProxy(endpointPort, proxyPort, targetDomain) {

    override fun run(): Unit = runBlocking {
        val urlManager = urlActor(startingUris)
        addUrls = {
            withTimeoutOrNull(1.seconds) {
                urlManager.send(SendUrls(it))
            }
        }
        manualParsing = buildMap {
            put(Regex("(?:<|&gt)form(?:(?!>|&lt).)*action=[\"']([^\"']*)[\"'](?:(?!>|&lt).)*(?:>|&lt)")) { seq ->
                seq.forEach { pattern ->
                    pattern.groupValues[1].let {
                        if (it.startsWith('/')) "https://localhost:$proxyPort$it" else it
                    }.let {
                        policyBuilder.policy.computeIfPresent("form-action") { _, directive ->
                            directive.adjustToUri(it, selfRegex)
                        }
                    }
                }
            }
        }
        delay(1.seconds)
        launch { endpointServer.start(wait = false) }
        launch { proxyServer.start(wait = false) }
        delay(1.seconds)
        launch {
            withTimeoutOrNull(timeoutMinutes.minutes) {
                while (true) {
                    CompletableDeferred<String?>().also { urlManager.send(GetUrl(it)) }.await()?.let {
                        "http://localhost:$proxyPort$it"
                    }?.let {
                        "Visiting $it".log()
                        browser.load(it)
                        delay(loadDelaySeconds.seconds)
                    } ?: break
                }
            }
            "Policy for $targetDomain:".log()
            browser.quit()
            urlManager.close()
            policyBuilder.policy.toPolicyString().log()
            endpointServer.stop(1, 1, TimeUnit.SECONDS)
            proxyServer.stop(1, 1, TimeUnit.SECONDS)
        }
    }

    private fun String.log() = also {
        println(it)
        logFile?.appendText("$it\n")
    }
}

sealed class UrlMsg
class SendUrls(val urls: Sequence<String>) : UrlMsg()
class GetUrl(val url: CompletableDeferred<String?>) : UrlMsg()

fun CoroutineScope.urlActor(initialQueue: Collection<String> = listOf()) = actor<UrlMsg> {
    val alreadyReceived: MutableSet<String> = initialQueue.toMutableSet()
    val queue = ArrayDeque(initialQueue)
    for (msg in channel) when (msg) {
        is SendUrls -> msg.urls.forEach {
            if (it !in alreadyReceived) {
                alreadyReceived.add(it)
                queue.addLast(it)
            }
        }
        is GetUrl -> msg.url.complete(queue.removeFirstOrNull())
    }
}