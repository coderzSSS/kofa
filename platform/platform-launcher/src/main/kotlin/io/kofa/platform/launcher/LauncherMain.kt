package io.kofa.platform.launcher

import io.kofa.platform.api.logger.logger
import io.kofa.platform.core.launcher.ConfigLoader
import io.kofa.platform.core.launcher.PlatformLauncher
import picocli.CommandLine
import java.lang.Thread.sleep
import java.util.concurrent.TimeUnit
import kotlin.Throws
import kotlin.concurrent.thread
import kotlin.system.exitProcess

fun main(vararg args: String) {
    val commandSpec = CommandLine.Model.CommandSpec.create()
    commandSpec.mixinStandardHelpOptions()
    commandSpec.addOption(
        CommandLine.Model.OptionSpec.builder("-c", "--config")
            .paramLabel("CONFIG")
            .type(List::class.java)
            .auxiliaryTypes(String::class.java)
            .description("application config files or resources")
            .build()
    )

    val commandLine = CommandLine(commandSpec)
    commandLine.setExecutionStrategy(LauncherMain::launch)
    commandLine.execute(*args)
}

internal object LauncherMain {
    private const val EXIT_TIMEOUT_SECONDS = 10L

    fun launch(pr: CommandLine.ParseResult): Int {
        val helpExitCode = CommandLine.executeHelpRequest(pr)
        if (helpExitCode != null) {
            return helpExitCode
        }

        val configFileList = pr.matchedOptionValue("c", emptyList<String>())

        return run {
            launch(configFileList)
            0
        }
    }

    @Throws(Exception::class)
    private fun launch(configFiles: List<String>) {
        val config = ConfigLoader.fromFile(configFiles)
        runCatching {
            PlatformLauncher.launch(config)
        }.onFailure(this::onLauncherException)
    }

    private fun onLauncherException(e: Throwable) {
        logger.error(e) {
            "Error caught while launching application, existing process with $EXIT_TIMEOUT_SECONDS seconds."
        }

        thread(isDaemon = false, name = "Kill-Thread") {
            runCatching {
                sleep(TimeUnit.SECONDS.toMillis(EXIT_TIMEOUT_SECONDS))
            }.onFailure { e ->
                logger.error(e) { "Kill thread interrupted" }
            }

            logger.warn {
                "The application is still running despite an exception, are there non-daemon threads keeping the process alive? Calling System.exit(1)"
            }

            exitProcess(1)
        }

        throw e
    }
}