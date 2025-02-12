package io.kofa.example.carnival.application

import io.kofa.example.carnival.business.MonkeyHandler
import io.kofa.platform.api.annotation.DomainModule

@DomainModule(componentType = "Monkey", handlerClass = MonkeyHandler::class)
object MonkeyModule {
}