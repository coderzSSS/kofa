package io.kofa.platform.core.internal.launcher

import io.kofa.platform.api.logger.logger
import io.kofa.platform.core.internal.controller.ApplicationController
import org.koin.core.Koin
import org.koin.dsl.koinApplication
import java.util.*
import kotlin.concurrent.thread
import kotlinx.datetime.TimeZone as KTimeZone

internal object PlatformLauncherInternal {
    internal fun launch(platformConfig: PlatformConfig) {
        setJvmDefaultTimeZone(platformConfig.timezone)

        startApplication(platformConfig)
    }

    private fun startApplication(platformConfig: PlatformConfig) {
        logger.info { "setup application injector" }
        val koin = setupInjector(platformConfig)

        val controller = ApplicationController(koin)

        logger.info { "configuring auto loaded components" }
        controller.configure(platformConfig.components)

        controller.start()
        logger.info { "application started" }

        Runtime.getRuntime().addShutdownHook(
            thread(start = false, name = "Platform-ShutdownHook") {
                logger.info { "platform shutdown hook triggered, stopping application" }
                controller.stop()
            }
        )
    }

    private fun setupInjector(config: PlatformConfig): Koin {
        return koinApplication {
            modules(platformModule(config))
        }.koin
    }

    private fun setJvmDefaultTimeZone(tz: KTimeZone?) {
        tz?.let {
            TimeZone.setDefault(TimeZone.getTimeZone(it.id))
        }
    }
}