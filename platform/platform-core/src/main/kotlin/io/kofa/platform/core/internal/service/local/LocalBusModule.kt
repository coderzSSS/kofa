package io.kofa.platform.core.internal.service.local

import io.kofa.platform.core.internal.media.LocalEventBus
import io.kofa.platform.core.internal.service.CommandBusService
import io.kofa.platform.core.internal.service.EventBusService
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import java.net.URI

internal fun buildLocalBusModule(uri: URI) = buildMap<String, Module> {
    val qualifier = named(uri.path)

    computeIfAbsent("local_bus_${uri.path}") { key ->
        module {
            single(qualifier) {
                LocalEventBus<Any>()
            }
        }
    }

    computeIfAbsent("local_cmd_bus_${uri.path}") { key ->
        module {
            single(qualifier) {
                LocalCommandBusService(get(qualifier))
            }.bind(CommandBusService::class)
        }
    }

    computeIfAbsent("local_event_bus_${uri.path}") { key ->
        module {
            single(qualifier) {
                LocalEventBusService(get(), get(qualifier), "LocalEventBusService")
            }.bind(EventBusService::class)
        }
    }

}