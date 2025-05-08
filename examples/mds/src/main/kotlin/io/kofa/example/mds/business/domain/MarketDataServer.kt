package io.kofa.example.mds.business.domain

import io.kofa.example.mds.generated.PriceTickEvent

interface MarketDataServer: MarketDataService, AutoCloseable {
    suspend fun start() {}

    fun subscribe(tickers: List<String>, action: suspend (List<PriceTickEvent>) -> Unit)

    fun subscribe(action: suspend (List<PriceTickEvent>) -> Unit)
}