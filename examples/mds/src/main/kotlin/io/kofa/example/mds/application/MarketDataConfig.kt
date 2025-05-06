package io.kofa.example.mds.application

data class MarketDataConfig (val subscribedTickers: Map<String, Double>, val mock: Boolean = true)
