package io.kofa.example.mds.application

import io.kofa.example.mds.business.MarketDataHandler
import io.kofa.platform.api.annotation.DomainModule
import io.kofa.platform.api.config.Config
import io.kofa.platform.api.config.getOrNull
import io.kofa.platform.api.inject.ComponentModuleDeclaration
import org.koin.dsl.bind

@DomainModule(componentType = "MarketData", handlerClass = MarketDataHandler::class)
object MarketDataModule {
    fun mdsConfig(): ComponentModuleDeclaration = {
        scoped {
            val config = get<Config>()
            val tickers = requireNotNull(config.getConfigMap("tickers", Double::class))
            val mock = config.getOrNull<Boolean>("mock") == true
            MarketDataConfig(tickers, mock)
        }.bind()
    }
}