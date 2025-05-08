package io.kofa.example.pms.business.domain

import io.kofa.example.mds.business.domain.MarketDataService
import io.kofa.example.mds.generated.PriceTickEvent
import io.kofa.example.pms.application.PortfolioConfig
import io.kofa.example.pms.business.domain.model.PositionInfo
import io.kofa.example.rds.business.Equity
import io.kofa.example.rds.business.Option
import io.kofa.example.rds.business.ProductService
import io.kofa.example.rds.business.PutCall
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.*
import kotlin.math.*

class PortfolioManager(
    private val portfolioConfig: PortfolioConfig,
    private val productService: ProductService,
    private val marketDataService: MarketDataService
) {
    private val portfolio = mutableMapOf<String, PositionInfo>()
    private val marketValueMathContext = MathContext(portfolioConfig.marketValueScale, RoundingMode.HALF_UP)

    fun addPortfolio(symbol: String, quantity: Long) {
        val product = productService.lookup(symbol)
        val price = marketDataService.fetch(symbol) ?: 0.0

        val positionInfo = PositionInfo(
            product = product,
            price = price,
            quantity = quantity,
            value = BigDecimal(0.0)
        )

        val result = portfolio.putIfAbsent(product.symbol, positionInfo)
        require(result == null) { "product $symbol already added" }
    }

    fun calculateMarketValue(priceTicks: List<PriceTickEvent>): List<PositionInfo> {
        return priceTicks.distinct().flatMap { priceTick ->
            portfolio.values.filter {
                when (val product = it.product) {
                    is Equity -> product.symbol == priceTick.symbol
                    is Option -> product.underlyingSymbol == priceTick.symbol
                }
            }.map { positionInfo -> doCalc(priceTick, positionInfo) }
        }.toList()
    }

    fun getView(): List<PositionInfo> {
        return portfolio.values.sortedBy { pos -> pos.product.symbol }.toList()
    }

    private fun doCalc(
        priceTick: PriceTickEvent,
        positionInfo: PositionInfo
    ): PositionInfo {
        check(positionInfo.quantity.absoluteValue > 0) { "invalid position: $positionInfo" }
        check(priceTick.price > 0) { "invalid price: $positionInfo" }

        val product = positionInfo.product

        val price = when (product) {
            is Equity -> priceTick.price
            is Option -> calcOptionPrice(product, priceTick.price)
        }

        val usedPrice = BigDecimal.valueOf(price).round(marketValueMathContext)
        val value = usedPrice.multiply(BigDecimal.valueOf(positionInfo.quantity))

        positionInfo.price = usedPrice.toDouble()
        positionInfo.value = value

        return positionInfo
    }

    private fun calcOptionPrice(option: Option, spotPrice: Double): Double {
        val callOrPut = option.putCall === PutCall.CALL
        val strikePrice: Double = option.strikePrice
        val riskFreeRate = 0.1
        val timeInYears: Int = option.year - Calendar.getInstance().get(Calendar.YEAR)
        val volatility = 0.1

        return calcOptionPrice(callOrPut, spotPrice, strikePrice, riskFreeRate, timeInYears * 1.0, volatility)
    }

    // copied from https://github.com/bret-blackford/black-scholes/blob/master/OptionValuation/src/mBret/options/BlackScholesFormula.java
    private fun calcOptionPrice(
        callOrPut: Boolean, spotPrice: Double, strikePrice: Double, riskFreeRate: Double,
        timeInYears: Double, volatility: Double
    ): Double {
        var blackScholesOptionPrice = 0.0

        if (callOrPut) {
            val cd1 = cumulativeDistribution(d1(spotPrice, strikePrice, riskFreeRate, timeInYears, volatility))
            val cd2 = cumulativeDistribution(d2(spotPrice, strikePrice, riskFreeRate, timeInYears, volatility))

            blackScholesOptionPrice = spotPrice * cd1 - strikePrice * exp(-riskFreeRate * timeInYears) * cd2
        } else {
            val cd1 = cumulativeDistribution(-d1(spotPrice, strikePrice, riskFreeRate, timeInYears, volatility))
            val cd2 = cumulativeDistribution(-d2(spotPrice, strikePrice, riskFreeRate, timeInYears, volatility))

            blackScholesOptionPrice = strikePrice * exp(-riskFreeRate * timeInYears) * cd2 - spotPrice * cd1
        }

        return blackScholesOptionPrice
    }

    private fun d1(s: Double, k: Double, r: Double, t: Double, v: Double): Double {
        val top = ln(s / k) + (r + v.pow(2.0) / 2) * t
        val bottom = v * sqrt(t)

        return top / bottom
    }

    private fun d2(s: Double, k: Double, r: Double, t: Double, v: Double): Double {
        return d1(s, k, r, t, v) - v * sqrt(t)
    }

    private fun cumulativeDistribution(x: Double): Double {
        val t = 1 / (1 + P * abs(x))
        val t1 = B1 * t.pow(1.0)
        val t2 = B2 * t.pow(2.0)
        val t3 = B3 * t.pow(3.0)
        val t4 = B4 * t.pow(4.0)
        val t5 = B5 * t.pow(5.0)
        val b = t1 + t2 + t3 + t4 + t5

        val snd = standardNormalDistribution(x) //for testing
        val cd = 1 - (snd * b)

        var resp = 0.0
        if (x < 0) {
            resp = 1 - cd
        } else {
            resp = cd
        }

        return resp
    }

    private fun standardNormalDistribution(x: Double): Double {
        val top = exp(-0.5 * x.pow(2.0))
        val bottom = sqrt(2 * Math.PI)
        val resp = top / bottom

        return resp
    }

    companion object {
        const val P: Double = 0.2316419
        const val B1: Double = 0.319381530
        const val B2: Double = -0.356563782
        const val B3: Double = 1.781477937
        const val B4: Double = -1.821255978
        const val B5: Double = 1.330274429
    }
}