package io.kofa.platform.core.internal.service.local

import io.kofa.platform.core.internal.media.LocalEventBus
import io.kofa.platform.core.internal.service.common.AbstractEventBusService
import io.kofa.platform.core.internal.thread.eventloop.EventLoop
import io.kofa.platform.core.internal.thread.eventloop.InvocationContext

internal class LocalEventBusService(
    val eventLoop: EventLoop, val localBus: LocalEventBus<MessageWrapper>, name: String
) : AbstractEventBusService(name) {
    override suspend fun start() {
        localBus.subscribe { eventWrapper ->
            val header = eventWrapper.header

            dispatch(eventWrapper.payload) {
                update(header.eventTimeStampInMillis)
            }
        }
    }

    override suspend fun stop() {
        localBus.close()
    }

    override suspend fun poll(context: InvocationContext): Boolean {
        // keep event loop running
        return true
    }
}