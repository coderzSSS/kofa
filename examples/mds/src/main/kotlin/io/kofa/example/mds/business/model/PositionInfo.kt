package io.kofa.example.mds.business.model

import java.math.BigDecimal

data class PositionInfo(
    val symbol: String,
    val price: Double,
    val quantity: Long,
    val value: BigDecimal
)
