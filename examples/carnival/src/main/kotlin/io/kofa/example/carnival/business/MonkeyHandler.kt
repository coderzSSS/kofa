package io.kofa.example.carnival.business

import io.kofa.example.carnival.v2.AppleMessage
import io.kofa.example.carnival.v2.BananaEvent
import io.kofa.platform.api.logger.Logger
import io.kofa.platform.api.util.MessageSender
import java.util.concurrent.atomic.AtomicInteger

class MonkeyHandler(
    private val logger: Logger,
    private val messageSender: MessageSender<Any>
) {
    val counter = AtomicInteger(0)

    suspend fun onBananaEvent(event: BananaEvent) {
        logger.info { "got banana ${event.name}" }
        messageSender.send(AppleMessage("${counter.incrementAndGet()}"))
    }
}