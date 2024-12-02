package io.kofa.example.carnival.business

import com.google.auto.service.AutoService
import io.kofa.example.carnival.domain.message.Banana
import io.kofa.example.carnival.domain.message.CarnivalEvent
import io.kofa.platform.api.dsl.BusinessDeclaration
import io.kofa.platform.api.dsl.inject
import io.kofa.platform.api.logger.Logger

@AutoService(BusinessDeclaration::class)
object Carnival: BusinessDeclaration<CarnivalEvent> ({
    component("Clown") {
        val logger: Logger by inject()

        onEvent(Banana::class) { event ->
            logger.info { "got banana ${event.name}" }
        }
    }
})