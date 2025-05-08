package io.kofa.example.rds.business

sealed interface Product {
    val symbol: String
}

data class Equity(
    override val symbol: String
): Product

enum class PutCall {
    PUT, CALL
}
data class Option(
    override val symbol: String,
    val putCall: PutCall,
    val strikePrice: Double,
    val year: Int,
    val month: Int,
    val underlyingSymbol: String
): Product