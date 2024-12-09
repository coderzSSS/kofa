package io.kofa.platform.core.internal.component.config

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kofa.platform.api.inject.ComponentModuleDeclaration
import io.kofa.platform.api.logger.Logger
import org.koin.dsl.bind

internal fun componentModule(config: ComponentConfig): ComponentModuleDeclaration = {
    scoped {
        KotlinLogging.logger(config.source)
    }.bind(Logger::class)

    scoped {
        config
    }
}