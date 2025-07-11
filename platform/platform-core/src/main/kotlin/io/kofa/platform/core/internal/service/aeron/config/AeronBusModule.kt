package io.kofa.platform.core.internal.service.aeron.config

import io.aeron.Aeron
import io.aeron.driver.MediaDriver
import io.kofa.platform.core.internal.launcher.DOMAIN_QUALIFIER
import io.kofa.platform.core.internal.service.CommandBusService
import io.kofa.platform.core.internal.service.EventBusService
import io.kofa.platform.core.internal.service.aeron.AeronCommandBusService
import io.kofa.platform.core.internal.service.aeron.AeronEventBusService
import io.kofa.platform.core.internal.service.config.EventStreamConfig
import io.kofa.platform.core.internal.service.config.getParameterValue
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import java.net.URI

internal fun buildAeronBusModule(uri: URI, config: EventStreamConfig) = buildMap<String, Module> {
    val qualifier = named(uri.path)

    val isEmbedded = uri.getParameterValue("embedded")?.toBoolean() ?: false

    val originConfig = config.config
    computeIfAbsent("aeron_bus_${uri.path}") { key ->
        module {
            single(qualifier) {
                val ctx = Aeron.Context()
                if (isEmbedded) {
                    val driver = MediaDriver.launchEmbedded()
                    ctx.aeronDirectoryName(driver.aeronDirectoryName())
                }

                ctx
            }
        }
    }

    if (uri == config.subscribeUri) {
        val finalPath = if (originConfig.hasPath("aeron-event")) "aeron-event" else "aeron"
        val aeronConfig = config.config.extract(AeronConfig::class, finalPath)
        computeIfAbsent("aeron_event_bus_${uri.path}") { key ->
            module {
                single {
                    val domain = get<String>(named(DOMAIN_QUALIFIER))
                    AeronEventBusService(
                        get(named(domain)),
                        get(qualifier),
                        aeronConfig.channel ?: uri.toString(),
                        aeronConfig.sessionId
                    )
                }.bind(EventBusService::class)
            }
        }
    }

    if (uri == config.publishUri) {
        val finalPath = if (originConfig.hasPath("aeron-cmd")) "aeron-cmd" else "aeron"
        val aeronConfig = config.config.extract(AeronConfig::class, finalPath)
        computeIfAbsent("aeron_cmd_bus_${uri.path}") { key ->
            module {
                single {
                    val domain = get<String>(named(DOMAIN_QUALIFIER))
                    AeronCommandBusService(
                        get(named(domain)),
                        get(qualifier),
                        aeronConfig.channel ?: uri.toString(),
                        aeronConfig.sessionId
                    )
                }.bind(CommandBusService::class)
            }
        }
    }
}