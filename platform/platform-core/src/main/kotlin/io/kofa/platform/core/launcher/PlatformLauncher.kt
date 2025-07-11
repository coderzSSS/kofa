package io.kofa.platform.core.launcher

import io.kofa.platform.api.config.Config
import io.kofa.platform.api.config.extract
import io.kofa.platform.api.config.extractOrNull
import io.kofa.platform.api.config.getOrDefault
import io.kofa.platform.api.config.getOrNull
import io.kofa.platform.api.logger.logger
import io.kofa.platform.core.internal.component.config.ComponentConfig
import io.kofa.platform.core.internal.launcher.PlatformConfig
import io.kofa.platform.core.internal.launcher.PlatformLauncherInternal
import io.kofa.platform.core.internal.service.config.EventStreamConfig
import io.kofa.platform.core.internal.service.config.EventStreamMode
import io.kofa.platform.core.internal.thread.eventloop.config.EventLoopConfig
import io.kofa.platform.core.internal.thread.eventloop.config.WaitExecutionPolicyConfig
import io.kofa.platform.core.internal.thread.policy.Busy
import io.kofa.platform.core.internal.thread.policy.ExecutionPolicy
import io.kofa.platform.core.internal.thread.policy.Wait
import kotlinx.datetime.TimeZone
import java.net.URI
import kotlin.time.DurationUnit
import kotlin.time.toDuration

object PlatformLauncher {
    fun launch(config: Config) {
        PlatformLauncherInternal.launch(resolvePlatformConfig(config))
    }

    private fun resolvePlatformConfig(config: Config): PlatformConfig {
        val domain = config.getString("domain")
        val timezone = config.getOrNull<String>("timezone")

        val eventLoopConfigOrigin = config.getConfig("event-loop")
        val shutdownTimeOut = eventLoopConfigOrigin.getLong("shutdown-timeout-seconds")

        val eventLoopConfig = EventLoopConfig(
            shutdownTimeoutSeconds = shutdownTimeOut,
            exitOnException = config.getOrDefault<Boolean>("exit-on-exception", true),
            executionPolicy = resolveExecutionPolicy(eventLoopConfigOrigin),
            threadRole = config.getOrDefault("thread-role", "EventLoop")
        )

        val componentConfigByType = config.getConfigMap("component").mapNotNull {
            val componentConfigOrigin = it.value
            val enabled = componentConfigOrigin.getOrDefault<Boolean>("enabled", true)
            val componentType = componentConfigOrigin.getOrDefault("type", it.key)
            val instanceName = componentConfigOrigin.getOrDefault<String>("instance-name", componentType.lowercase())
            val readOnly = componentConfigOrigin.getOrDefault<Boolean>("read-only", false)
            val allowMultiInstance = componentConfigOrigin.getOrDefault<Boolean>("allow-multiple-instance", false)

            if (enabled) {
                ComponentConfig(
                    type = componentType,
                    instanceName = instanceName,
                    allowMultipleInstance = allowMultiInstance,
                    readOnly = readOnly,
                    config = componentConfigOrigin
                )
            } else {
                logger.warn { "component $componentType is not enabled, skipping..." }
                null
            }
        }.associateBy { it.type }

        val eventStreamConfigOrigin = config.getConfig("event-stream")
        val publishUrl = eventStreamConfigOrigin.getOrNull<String>("publish-url") ?: eventStreamConfigOrigin.getString("url")
        val subscribeUrl = eventStreamConfigOrigin.getOrNull<String>("subscribe-url") ?: eventStreamConfigOrigin.getString("url")

        val eventStreamConfig = EventStreamConfig(
            mode = eventStreamConfigOrigin.extractOrNull<EventStreamMode>("mode") ?: EventStreamMode.Auto,
            publishUri = URI(publishUrl),
            subscribeUri = URI(subscribeUrl),
            config = eventStreamConfigOrigin
        )

        return PlatformConfig(
            domain = domain,
            timezone = timezone?.let { tz ->  TimeZone.of(tz)} ?: TimeZone.currentSystemDefault(),
            eventLoop = eventLoopConfig,
            eventStream = eventStreamConfig,
            components = componentConfigByType.values.toList(),
            config
        )
    }

    private fun resolveExecutionPolicy(eventLoopConfig: Config): ExecutionPolicy {
        val executionPolicyType = eventLoopConfig.getString("execution-policy")
        return when (executionPolicyType.lowercase()) {
            "busy" -> Busy
            "wait" -> {
                val c: WaitExecutionPolicyConfig = eventLoopConfig.extract()
                Wait(c.sleepCycles, c.durationMillis.toDuration(DurationUnit.MILLISECONDS))
            }

            else -> throw IllegalArgumentException("unknown execution policy $executionPolicyType")
        }
    }
}