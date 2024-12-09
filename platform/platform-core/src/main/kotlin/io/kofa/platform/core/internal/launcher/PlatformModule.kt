package io.kofa.platform.core.internal.launcher

import io.kofa.platform.core.internal.service.config.eventStreamModule
import io.kofa.platform.core.internal.service.meta.MessageMetaRegistry
import io.kofa.platform.core.internal.thread.eventloop.config.eventLoopModule
import kotlinx.serialization.StringFormat
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

internal fun platformModule(config: PlatformConfig) = module {
    single(named("domain")) {
        config.domain
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