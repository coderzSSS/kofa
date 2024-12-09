package io.kofa.platform.core.internal.service.config

import io.kofa.platform.core.internal.media.LocalEventBus
import io.kofa.platform.core.internal.service.CommandBusService
import io.kofa.platform.core.internal.service.EventBusService
import io.kofa.platform.core.internal.service.local.LocalCommandBusService
import io.kofa.platform.core.internal.service.local.LocalEventBusService
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import java.net.URI

internal fun eventStreamModule(config: EventStreamConfig): Module {
    val modules = mutableMapOf<String, Module>()

    setOf(config.subscribeUri, config.publishUri).forEach { uri ->
        buildBusModule(uri, modules)
    }

    return module {
        single {
            config
        }

        includes(modules.values)
    }
}

private fun buildBusModule(uri: URI, modules: MutableMap<String, Module>) {
    val protocol = uri.scheme
    val mediaType = requireNotNull(MediaType.entries.find { it.protocol == protocol }) { "unknown protocol: $protocol" }

    val result: Map<String, Module> = when (mediaType) {
        MediaType.InProcess -> {
            buildLocalBusModule(uri)
        }

        else -> {
            throw IllegalArgumentException("unsupported media type: $mediaType")
        }
    }

    result.forEach { (key, value) -> modules.putIfAbsent(key, value) }
}

private fun buildLocalBusModule(uri: URI) = buildMap<String, Module> {
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
                LocalEventBusService(get(qualifier), "LocalEventBusService")
            }.bind(EventBusService::class)
        }
    }

}