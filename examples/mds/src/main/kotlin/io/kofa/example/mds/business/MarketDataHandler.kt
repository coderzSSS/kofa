package io.kofa.example.mds.business

import io.kofa.example.mds.business.domain.MarketDataServer
import io.kofa.example.mds.generated.MarketDataMessageHandler
import io.kofa.example.mds.generated.PriceTicksMessage

class MarketDataHandler(val marketDataServer: MarketDataServer) : MarketDataMessageHandler() {
    override suspend fun onStartup() {
        marketDataServer.start()

        marketDataServer.subscribe { tickers ->
            sendMessage(PriceTicksMessage(tickers = tickers.toMutableList(), strings = mutableListOf()))
        }
    }

    override suspend fun onShutdown() {
        marketDataServer.close()
    }
}