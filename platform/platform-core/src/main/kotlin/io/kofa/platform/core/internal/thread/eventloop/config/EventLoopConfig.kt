package io.kofa.platform.core.internal.thread.eventloop.config

import io.kofa.platform.api.config.Config
import io.kofa.platform.core.internal.thread.policy.Busy
import io.kofa.platform.core.internal.thread.policy.ExecutionPolicy
import io.kofa.platform.core.internal.thread.policy.Wait
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import io.kofa.platform.api.config.extract

internal data class EventLoopConfig(
    val shutdownTimeoutSeconds: Long,
    val exitOnException: Boolean = true,
    val executionPolicyType: String,
    val config: Config,
    val threadRole: String? = "EventLoop"
)

internal val EventLoopConfig.executionPolicy: ExecutionPolicy
    get() {
        return when (executionPolicyType.lowercase()) {
            "busy" -> Busy
            "wait" -> {
                val c: WaitExecutionPolicyConfig = config.extract()
                Wait(c.sleepCycles, c.durationMillis.toDuration(DurationUnit.MILLISECONDS))
            }

            else -> throw IllegalArgumentException("unknown execution policy $executionPolicyType")
        }
    }

internal data class WaitExecutionPolicyConfig(
    val sleepCycles: Long,
    val durationMillis: Long
)

