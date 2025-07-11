package io.kofa.platform.core.internal.service.config

import io.kofa.platform.core.internal.service.aeron.config.buildAeronBusModule
import io.kofa.platform.core.internal.service.local.buildLocalBusModule
import org.koin.core.module.Module
import org.koin.dsl.module
import java.net.URI

internal fun eventStreamModule(config: EventStreamConfig): Module {
    val modules = mutableMapOf<String, Module>()

    setOf(config.subscribeUri, config.publishUri).forEach { uri ->
        buildBusModule(uri, config,modules)
    }

    return module {
        single {
            config
        }

        includes(modules.values)
    }
}

private fun buildBusModule(uri: URI, config: EventStreamConfig, modules: MutableMap<String, Module>) {
    val protocol = uri.scheme
    val mediaType = requireNotNull(MediaType.entries.find { it.protocol == protocol }) { "unknown protocol: $protocol" }

    val result: Map<String, Module> = when (mediaType) {
        MediaType.InProcess -> {
            buildLocalBusModule(uri, config)
        }

        MediaType.Aeron -> {
            buildAeronBusModule(uri, config)
        }

        else -> {
            throw IllegalArgumentException("unsupported media type: $mediaType")
        }
    }

    result.forEach { (key, value) -> modules.putIfAbsent(key, value) }
}

internal fun URI.getParameterValue(parameterName: String): String? {
    return rawQuery.split('&').map {
        val parts = it.split('=')
        val name = parts.firstOrNull() ?: ""
        val value = parts.drop(1).firstOrNull() ?: ""
        Pair(name, value)
    }.firstOrNull { it.first == parameterName }?.second
}

