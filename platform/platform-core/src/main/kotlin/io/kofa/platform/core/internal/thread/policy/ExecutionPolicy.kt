package io.kofa.platform.core.internal.thread.policy

import io.kofa.platform.api.logger.logger
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.toJavaDuration

internal sealed interface ExecutionPolicy {
    suspend fun onEndOfEachCycle(hasWorkLeft: Boolean)
}

internal object Busy : ExecutionPolicy {
    override suspend fun onEndOfEachCycle(hasWorkLeft: Boolean) {
        // noop
    }
}

internal class Wait(private val sleepCycles: Long, private val duration: Duration) : ExecutionPolicy {
    private var cycleCount: Long = 0

    override suspend fun onEndOfEachCycle(hasWorkLeft: Boolean) {
        cycleCount++

        if (hasWorkLeft || cycleCount % sleepCycles != 0L) {
            return
        }

        runCatching {
            logger.trace { "Wait for $duration" }
            delay(duration.toJavaDuration().toMillis())
            cycleCount = 0
        }
    }
}

