package io.kofa.example.mds.business.domain.mock

data class MockMarketDataConfig(
    val initialTickers: Map<String, Double>,
    val mockServerInitialDelayMillis: Long,
    val mockServerIntervalMillis: Long
)