package io.kofa.example.rds.business

interface ProductService {
    fun lookup(symbol: String): Product = requireNotNull(tryLookup(symbol)) {
        "no product found for $symbol"
    }

    fun tryLookup(symbol: String): Product?
}