package io.kofa.example.carnival.business.handler

import io.kofa.example.carnival.application.ClownConfig
import io.kofa.example.carnival.domain.message.CarnivalEvent
import io.kofa.example.carnival.domain.message.CarnivalEvent.Banana
import io.kofa.example.carnival.generated.CarnivalMessageHandler
import io.kofa.platform.api.logger.Logger
import io.kofa.platform.api.util.MessageSender
import java.util.concurrent.atomic.AtomicInteger

class ClownHandler(private val config: ClownConfig,
                   private val logger: Logger,
                   private val sender: MessageSender<Any>): CarnivalMessageHandler {
    val counter = AtomicInteger(0)
    override suspend fun onBananaEvent(event: CarnivalEvent.Banana) {
    }

    override suspend fun onAppleEvent(event: CarnivalEvent.Apple) {
        logger.info { "got apple ${event.name}" }
        if (event.name.toInt() < config.limit) {
            sender.send(Banana("${counter.incrementAndGet()}"))
        } else {
            logger.info { "I'm full !!" }
        }
    }
}