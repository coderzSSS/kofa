package io.kofa.platform.core.internal.service.local

import io.kofa.platform.core.internal.media.LocalEventBus
import io.kofa.platform.core.internal.service.CommandBusService
import io.kofa.platform.core.internal.service.EventBusService
import io.kofa.platform.core.internal.service.config.EventStreamConfig
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import java.net.URI

internal fun buildLocalBusModule(uri: URI, config: EventStreamConfig) = buildMap<String, Module> {
    val qualifier = named(uri.path)

    computeIfAbsent("local_bus_${uri.path}") { key ->
        module {
            single(qualifier) {
                LocalEventBus<Any>()
            }
        }
    }

    if (uri == config.subscribeUri) {
        computeIfAbsent("local_event_bus_${uri.path}") { key ->
            module {
                single {
                    LocalEventBusService(get(), get(qualifier), "LocalEventBusService")
                }.bind(EventBusService::class)
            }
        }
    }

    if (uri == config.publishUri) {
        computeIfAbsent("local_cmd_bus_${uri.path}") { key ->
            module {
                single {
                    LocalCommandBusService(get(qualifier))
                }.bind(CommandBusService::class)
            }
        }
    }

}