package io.kofa.example.mds.business.domain

interface MarketDataService {
    fun fetch(ticker: String): Double?
}