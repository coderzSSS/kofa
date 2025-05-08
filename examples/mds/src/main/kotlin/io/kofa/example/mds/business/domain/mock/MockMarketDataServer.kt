package io.kofa.example.mds.business.domain.mock

import io.kofa.example.mds.business.domain.MarketDataServer
import io.kofa.example.mds.generated.PriceTickEvent
import io.kofa.example.mds.generated.PriceTickMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.math.MathContext
import java.math.RoundingMode
import kotlin.random.Random

class MockMarketDataServer(
    initialData: Map<String, Double>,
    initialDelayMills: Long = 1000,
    intervalMills: Long = 2000
) : MarketDataServer {
    private val data = mutableMapOf<String, Double>()
    private val subscribers = mutableMapOf<Set<String>, MutableSet<suspend (List<PriceTickEvent>) -> Unit>>()
    private val globalSubscribers = mutableSetOf<suspend (List<PriceTickEvent>) -> Unit>()

    init {
        data.putAll(initialData)
    }

    private val worker = flow {
        delay(initialDelayMills)
        while (true) {
            emit(Unit)
            delay(intervalMills)
        }
    }

    private var mockJob: Job? = null

    private suspend fun mockPriceTick() {
        val tickers = data.keys.toList()
        val random = Random(System.currentTimeMillis())

        val size = tickers.size
        val randomTickSize = random.nextInt(1, size + 1);

        val indexes = buildSet {
            repeat(randomTickSize) {
                add(random.nextInt(size))
            }
        }

        val ticks = buildList {
            indexes.forEach { index ->
                val changePercent = random.nextInt(30)
                val ticker = tickers[index];
                val initPrice = requireNotNull(data[ticker]) { "no initial price found for $ticker" }

                val upOrDown = random.nextBoolean();
                val sign = if (upOrDown) 1 else -1

                val newPrice = initPrice * (1 + sign * changePercent / 100.0)

                if (newPrice.compareTo(initPrice) != 0) {
                    add(PriceTickMessage(ticker, newPrice.toBigDecimal().setScale(2, RoundingMode.HALF_UP).toDouble()))
                }
            }
        }

        if (ticks.isNotEmpty()) {
            subscribers.forEach { (tickers, actions) ->
                val events = ticks.filter { event -> tickers.contains(event.symbol) }
                if (events.isNotEmpty()) {
                    actions.forEach { it(events) }
                }
            }

            globalSubscribers.forEach { it(ticks) }
        }
    }

    override suspend fun start() {
        mockJob = worker.onEach {
            mockPriceTick()
        }.launchIn(CoroutineScope(SupervisorJob() + Dispatchers.IO))
    }

    override fun subscribe(
        tickers: List<String>,
        action: suspend (List<PriceTickEvent>) -> Unit
    ) {
        subscribers.computeIfAbsent(tickers.toSet()) { HashSet() }.add(action)
    }

    override fun subscribe(action: suspend (List<PriceTickEvent>) -> Unit) {
        globalSubscribers.add(action)
    }

    override fun fetch(ticker: String): Double? {
        return data[ticker]
    }

    override fun close() {
        mockJob?.cancel()
        subscribers.clear()
    }
}