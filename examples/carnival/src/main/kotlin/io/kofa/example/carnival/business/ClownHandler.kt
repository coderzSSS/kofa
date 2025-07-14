package io.kofa.example.carnival.business

import io.kofa.example.carnival.application.ClownConfig
import io.kofa.example.carnival.v2.AppleEvent
import io.kofa.example.carnival.v2.BananaMessage
import io.kofa.example.carnival.v2.CarnivalMessageHandler
import io.kofa.platform.api.inject.inject
import io.kofa.platform.api.util.TaskUtil
import java.util.concurrent.atomic.AtomicInteger

class ClownHandler(
    private val clownConfig: ClownConfig,
) : CarnivalMessageHandler() {
    val counter = AtomicInteger(0)
    val taskUtil: TaskUtil by inject()

    override suspend fun onStartup() {
        logger.info { "sending banana ${counter.get() + 1}" }
        taskUtil.scheduleRun("send first banana") {
            messageSender.send(BananaMessage("${counter.incrementAndGet()}"))
        }
    }

    override suspend fun onAppleEvent(event: AppleEvent) {
        logger.info { "got apple ${event.name}" }
        if ((event.name?.toInt() ?: 0) < clownConfig.limit) {
            messageSender.send(BananaMessage("${counter.incrementAndGet()}"))
        } else {
            logger.info { "I'm full !!" }
        }
    }
}