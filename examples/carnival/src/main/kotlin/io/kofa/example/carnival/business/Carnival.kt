package io.kofa.example.carnival.business

import com.google.auto.service.AutoService
import io.kofa.example.carnival.domain.message.CarnivalEvent
import io.kofa.example.carnival.domain.message.CarnivalEvent.Apple
import io.kofa.example.carnival.domain.message.CarnivalEvent.Banana
import io.kofa.platform.api.dsl.BusinessDeclaration
import io.kofa.platform.api.dsl.inject
import io.kofa.platform.api.logger.Logger
import io.kofa.platform.api.util.MessageSender
import java.util.concurrent.atomic.AtomicInteger

@AutoService(BusinessDeclaration::class)
@SuppressWarnings("rawtypes")
class Carnival: BusinessDeclaration<CarnivalEvent> ({
    component("Clown") {
        val logger: Logger by inject()
        val sender: MessageSender<CarnivalEvent> by inject()
        val counter = AtomicInteger(0)

        onStart {
            logger.info { "sending banana ${counter.get() + 1}"}
            sender.send(Banana("${counter.incrementAndGet()}"))
        }

        onEvent(Apple::class) { event ->
            logger.info { "got apple ${event.name}" }
            sender.send(Banana("${counter.incrementAndGet()}"))
        }
    }

    component("Monkey") {
        val logger: Logger by inject()
        val sender: MessageSender<CarnivalEvent> by inject()
        val counter = AtomicInteger(0)

        onEvent(Banana::class) { event ->
            logger.info { "got banana ${event.name}" }
            sender.send(Apple("${counter.incrementAndGet()}"))
        }
    }
})