package io.kofa.example.carnival.business.handler

import io.kofa.example.carnival.application.ClownConfig
import io.kofa.example.carnival.domain.message.CarnivalEvent
import io.kofa.example.carnival.domain.message.CarnivalEvent.Banana
import io.kofa.example.carnival.generated.CarnivalMessageHandler
import java.util.concurrent.atomic.AtomicInteger

class ClownHandler(
    private val clownConfig: ClownConfig
) : CarnivalMessageHandler() {
    val counter = AtomicInteger(0)

    override suspend fun onAppleEvent(event: CarnivalEvent.Apple) {
        logger.info { "got apple ${event.name}" }
        if (event.name.toInt() < clownConfig.limit) {
            messageSender.send(Banana("${counter.incrementAndGet()}"))
        } else {
            logger.info { "I'm full !!" }
        }
    }
}