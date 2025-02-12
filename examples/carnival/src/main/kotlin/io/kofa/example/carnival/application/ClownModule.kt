package io.kofa.example.carnival.application

import io.kofa.example.carnival.business.ClownHandler
import io.kofa.platform.api.annotation.DomainModule
import io.kofa.platform.api.config.Config
import io.kofa.platform.api.inject.ComponentModuleDeclaration
import org.koin.core.qualifier.named
import org.koin.dsl.bind

@DomainModule(componentType = "Clown", handlerClass = ClownHandler::class)
object ClownModule {
    fun clownConfig(): ComponentModuleDeclaration = {
        scoped {
            val config = get<Config>()
            ClownConfig(config.getInt("maxValue"))
        }.bind()

        scoped(named("limit")) {
            val config = get<ClownConfig>()
            config.limit
        }.bind()
    }
}