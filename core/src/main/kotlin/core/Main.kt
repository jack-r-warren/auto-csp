package core

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import java.io.File
import java.net.ServerSocket

fun main(args: Array<String>) =
    CommandRunner.subcommands(
        ProxyRunner,
        EndpointAndProxyRunner,
        AutomatedBrowserRunner
    ).main(args)

object CommandRunner : CliktCommand() {
    override fun run() = Unit
}

object ProxyRunner : CliktCommand(name = "proxy") {
    private val proxyPort: Int by option("--proxy-port").int().required()
    private val targetDomain: String by option("--target-domain").required()
    override fun run() = Proxy(
        proxyPort = proxyPort,
        targetDomain = targetDomain
    ).run()
}

object EndpointAndProxyRunner : CliktCommand(name = "endpoint-and-proxy") {
    private val proxyPort: Int by option("--proxy-port").int().required()
    private val targetDomain: String by option("--target-domain").required()
    override fun run() {
        EndpointAndProxy(
            ServerSocket(0).use { it.localPort },
            proxyPort,
            targetDomain
        ).run()
    }
}

object AutomatedBrowserRunner : CliktCommand(name = "automated-browser") {
    private val proxyPort: Int by option("--proxy-port").int().required()
    private val targetDomains: List<String> by option("--target-domain").multiple(required = true)
    private val alternateStartingUris: List<String> by option("--alternate-start").multiple(default = listOf("/"))
    private val browser: Browser by option("--browser").choice(
        "chrome" to Browser.Chrome(),
        "firefox" to Browser.Firefox()
    ).default(Browser.Chrome())
    private val loadDelay: Int by option("--delay").int().default(2)
    private val timeoutMinutes: Int by option("--timeout").int().default(10)
    private val logTo: File? by option("--log").file(fileOkay = true, folderOkay = false)
    override fun run() {
        logTo?.let {if (it.exists()) {
            it.copyTo(File(it.nameWithoutExtension + "-old.txt"))
            it.delete()
        }}
        for (targetDomain in targetDomains) {
                AutomatedBrowser(
                    ServerSocket(0).use { it.localPort },
                    proxyPort,
                    targetDomain,
                    alternateStartingUris,
                    browser,
                    loadDelay,
                    timeoutMinutes,
                    logTo
                ).run()
        }
    }
}
