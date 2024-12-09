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
    val exitOnException: Boolean,
    val executionPolicy: ExecutionPolicy,
    val threadRole: String?
)

internal data class WaitExecutionPolicyConfig(
    val sleepCycles: Long,
    val durationMillis: Long
)

