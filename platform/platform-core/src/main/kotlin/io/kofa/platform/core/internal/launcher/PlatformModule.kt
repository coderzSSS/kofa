package io.kofa.platform.core.internal.launcher

import io.kofa.platform.core.internal.service.config.eventStreamModule
import io.kofa.platform.core.internal.service.meta.MessageMetaRegistry
import io.kofa.platform.core.internal.thread.eventloop.config.eventLoopModule
import kotlinx.serialization.StringFormat
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

const val DOMAIN_QUALIFIER: String = "domain"

internal fun platformModule(config: PlatformConfig) = module {
    single(named(DOMAIN_QUALIFIER)) {
        config.domain
    }

    single {
        config.config
    }

    single {
        config
    }

    includes(
        jsonCodecModule(),
        fixCodecModule(),
        eventLoopModule(config.eventLoop),
        eventStreamModule(config.eventStream)
    )
}

private fun jsonCodecModule() = module {
    factory {
        Json {
            useAlternativeNames = false
            coerceInputValues = false
            serializersModule = MessageMetaRegistry.getMessageCodecModule()
        }
    }.bind(StringFormat::class)
}

private fun fixCodecModule() = module {

}