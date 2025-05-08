package io.kofa.example.pms.business.domain.model

import io.kofa.example.rds.business.Product
import java.math.BigDecimal

data class PositionInfo(
    val product: Product,
    var price: Double,
    val quantity: Long,
    var value: BigDecimal
)
