package io.kofa.example.mds.application

import io.kofa.example.mds.business.MarketDataHandler
import io.kofa.example.mds.business.domain.MarketDataServer
import io.kofa.example.mds.business.domain.MarketDataService
import io.kofa.example.mds.business.domain.mock.MockMarketDataConfig
import io.kofa.example.mds.business.domain.mock.MockMarketDataServer
import io.kofa.platform.api.annotation.DomainModule
import io.kofa.platform.api.config.Config
import io.kofa.platform.api.config.getOrNull
import io.kofa.platform.api.inject.ComponentModuleDeclaration
import org.koin.dsl.bind
import org.koin.dsl.binds

@DomainModule(componentType = "MarketData", handlerClass = MarketDataHandler::class)
object MarketDataModule {
    fun mdsServiceModule(): ComponentModuleDeclaration = {
        scoped {
            val config = get<Config>()
            val mock = config.getOrNull<Boolean>("mock") == true

            if (mock) {
                val tickers = requireNotNull(config.getConfigMap("tickers", Double::class))
                val delay = config.getOrNull<Long>("delayMills") ?: 1000
                val interval = config.getOrNull<Long>("intervalMills") ?: 2000

                val mockConfig = MockMarketDataConfig(tickers, delay, interval)
                MockMarketDataServer(
                    mockConfig.initialTickers,
                    mockConfig.mockServerInitialDelayMillis,
                    mockConfig.mockServerIntervalMillis
                )
            } else {
                throw UnsupportedOperationException("only support mocked MDS server at the moment")
            }

        }.bind(MarketDataServer::class).bind(MarketDataService::class)
    }
}