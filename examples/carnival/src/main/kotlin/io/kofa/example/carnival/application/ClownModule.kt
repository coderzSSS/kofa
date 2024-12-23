package io.kofa.example.carnival.application

import io.kofa.example.carnival.business.handler.ClownHandler
import io.kofa.example.carnival.generated.CarnivalMessageHandler
import io.kofa.platform.api.config.Config
import io.kofa.platform.api.inject.ComponentModuleDeclaration
import org.koin.core.qualifier.named
import org.koin.dsl.bind

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

    fun clownHandler(): ComponentModuleDeclaration = {
        scoped {
            ClownHandler(get())
        }.bind(CarnivalMessageHandler::class)
    }
}