package io.kofa.example.pms.application

import io.kofa.example.pms.business.PortfolioViewHandler
import io.kofa.example.pms.business.domain.PortfolioManager
import io.kofa.platform.api.annotation.DomainModule
import io.kofa.platform.api.config.Config
import io.kofa.platform.api.config.getOrNull
import io.kofa.platform.api.inject.ComponentModuleDeclaration
import org.koin.dsl.bind
import java.io.File

@DomainModule(componentType = "PMS", handlerClass = PortfolioViewHandler::class)
object PortfolioModule {
    fun pmsModule(): ComponentModuleDeclaration = {
        scoped {
            val config = get<Config>()
            val scale = config.getOrNull<Int>("scale") ?: 2

            linkTo(getScope("RefData"), getScope("MarketData"))
            PortfolioManager(PortfolioConfig(scale), get(), get())
        }.bind()

        scoped {
            val config = get<Config>()
            val positionFile = javaClass.classLoader.getResource(config.getString("positionFile"))?.file!!
            File(positionFile)
        }.bind()
    }
}