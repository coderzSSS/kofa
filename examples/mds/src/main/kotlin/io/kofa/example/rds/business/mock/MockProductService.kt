package io.kofa.example.rds.business.mock

import io.kofa.example.rds.business.*
import io.kofa.example.rds.business.Equity
import io.kofa.example.rds.business.Option
import io.kofa.example.rds.business.Product
import io.kofa.example.rds.business.PutCall
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.rows
import org.jetbrains.kotlinx.dataframe.io.readCSV

class MockProductService(equityProductFile: String, optionProductFile: String) : ProductService {
    private val products = mutableMapOf<String, Product>()

    init {
        val equity = DataFrame.readCSV(equityProductFile)
        equity.rows().forEach { row ->
            val ticker = row[0] as String
            products.put(ticker, Equity(ticker))
        }

        val options = DataFrame.readCSV(optionProductFile)

        options.rows().forEach { row ->
            val ticker = row[0] as String
            val putCall = row[2] as String
            val strikePrice = row[3] as Double
            val year = row[4] as Int
            val month = row[5] as Int
            val underlying = row[6] as String
            products.put(
                ticker,
                Option(ticker, PutCall.valueOf(putCall), strikePrice, year, month, underlying)
            )
        }

    }

    override fun tryLookup(symbol: String): Product? {
        return products[symbol]
    }
}