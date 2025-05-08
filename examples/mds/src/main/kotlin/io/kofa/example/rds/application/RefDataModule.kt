package io.kofa.example.rds.application

import io.kofa.example.rds.business.ProductService
import io.kofa.example.rds.business.mock.MockProductService
import io.kofa.platform.api.annotation.DomainModule
import io.kofa.platform.api.config.Config
import io.kofa.platform.api.config.getOrNull
import io.kofa.platform.api.inject.ComponentModuleDeclaration
import org.koin.dsl.bind

@DomainModule(componentType = "RefData")
object RefDataModule {
    fun rdsConfig(): ComponentModuleDeclaration = {
        scoped {
            val config = get<Config>()
            val mock = config.getOrNull<Boolean>("mock") == true
            if (mock) {
                val equityFile = javaClass.classLoader.getResource(config.getString("equityFile"))?.file!!
                val optionFile = javaClass.classLoader.getResource(config.getString("optionFile"))?.file!!
                MockProductService(equityFile, optionFile)
            } else {
                throw UnsupportedOperationException("only mocked product service available at the moment")
            }
        }.bind(ProductService::class)
    }
}