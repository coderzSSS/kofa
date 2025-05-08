package io.kofa.example.pms.business

import io.kofa.example.mds.generated.MarketDataMessageHandler
import io.kofa.example.mds.generated.PriceTicksEvent
import io.kofa.example.pms.business.domain.PortfolioManager
import io.kofa.example.pms.business.domain.PortfolioPrinter
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.rows
import org.jetbrains.kotlinx.dataframe.io.readCSV
import java.io.File

class PortfolioViewHandler(private val positionFile: File, private val portfolioManager: PortfolioManager) : MarketDataMessageHandler() {
    private val printer = PortfolioPrinter(System.out)

    override suspend fun onStartup() {
        val position = DataFrame.readCSV(positionFile)
        position.rows().forEach { row ->
            val symbol = row[0] as String
            val quantity = row[1] as Int
            portfolioManager.addPortfolio(symbol, quantity.toLong())
        }
    }
    override suspend fun onPriceTicksEvent(event: PriceTicksEvent) {
        val updated = portfolioManager.calculateMarketValue(event.tickers)

        if (updated.isNotEmpty()) {
            // print to console
            printer.print(event.tickers, portfolioManager.getView())
        }
    }
}