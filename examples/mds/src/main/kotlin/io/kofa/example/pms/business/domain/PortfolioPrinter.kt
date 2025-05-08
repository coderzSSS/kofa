package io.kofa.example.pms.business.domain

import io.kofa.example.mds.generated.PriceTickEvent
import io.kofa.example.pms.business.domain.model.PositionInfo
import java.io.PrintStream
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer

class PortfolioPrinter(private val printStream: PrintStream) {
    private val counter = AtomicInteger(0)

    fun print(
        priceTickList: List<PriceTickEvent>,
        positionInfoList: List<PositionInfo>
    ) {
        val count = counter.incrementAndGet()

        printStream.printf("## %d Market Data Update%n", count)
        priceTickList.forEach(Consumer { priceTick ->
            printStream.printf("%s change to %s %n", priceTick.symbol, priceTick.price)
        })

        printStream.println()
        printStream.printf("## Portfolio%n")
        val fmtStr = "%-25s %15s %15s %15s%n"
        printStream.printf(fmtStr, "symbol", "price", "qty", "value")

        positionInfoList.sortedBy { pos -> pos.product.symbol }
            .forEach { positionInfo ->
                printStream.printf(
                    fmtStr,
                    positionInfo.product.symbol,
                    positionInfo.price,
                    positionInfo.quantity,
                    positionInfo.value
                )
            }

        printStream.println()

        val value: BigDecimal? = positionInfoList
            .map { pos -> pos.value }
            .reduce { acc, v -> acc + v }

        printStream.printf("#Total portfolio %56s%n%n", value)
    }
}
