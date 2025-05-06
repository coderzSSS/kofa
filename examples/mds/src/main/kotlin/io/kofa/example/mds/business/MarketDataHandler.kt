package io.kofa.example.mds.business

import io.kofa.example.mds.application.MarketDataConfig
import io.kofa.example.mds.generated.MarketDataMessageHandler
import io.kofa.example.mds.generated.PriceTickEvent
import io.kofa.example.mds.generated.PriceTickMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.math.BigDecimal
import java.math.MathContext
import kotlin.random.Random

class MarketDataHandler(val mdsConfig: MarketDataConfig) : MarketDataMessageHandler() {
    private val worker = flow {
        delay(1000)
        while (true) {
            emit(Unit)
            delay(2000)
        }
    }

    private var mockJob: Job? = null

    private suspend fun mockPriceTick() {
        val tickers = mdsConfig.subscribedTickers.keys.toList()
        val random = Random(System.currentTimeMillis())

        val size = tickers.size
        val randomTickSize = random.nextInt(1, size + 1);

        val indexes = buildSet {
            repeat(randomTickSize) {
                add(random.nextInt(size))
            }
        }

        indexes.forEach { index ->
            val changePercent = random.nextInt(30)
            val ticker = tickers[index];
            val initPrice = requireNotNull(mdsConfig.subscribedTickers[ticker]) { "no initial price found for $ticker" }

            val upOrDown = random.nextBoolean();
            val sign = if (upOrDown) 1 else -1

            val newPrice = initPrice * (1 + sign * changePercent / 100.0)

            if (newPrice.compareTo(initPrice) != 0) {
                sendMessage(PriceTickMessage(ticker, BigDecimal(newPrice, MathContext(2)).toDouble()))
            }
        }
    }

    override suspend fun onStartup() {
        mockJob = worker.onEach {
            mockPriceTick()
        }.launchIn(CoroutineScope(SupervisorJob() + Dispatchers.IO))
    }

    override suspend fun onShutdown() {
        mockJob?.cancel()
    }

    override suspend fun onPriceTickEvent(event: PriceTickEvent) {
        println("Price tick event $event")
    }
}