package io.kofa.platform.core.internal.service.local

import io.kofa.platform.core.internal.media.LocalEventBus
import io.kofa.platform.core.internal.service.CommandBusService
import io.kofa.platform.core.internal.service.CommandHeader
import io.kofa.platform.core.internal.service.EventHeader

internal class LocalCommandBusService(private val localBus: LocalEventBus<MessageWrapper>) : CommandBusService {
    override suspend fun publish(header: CommandHeader, command: Any) {
        localBus.publish(MessageWrapper(toEventHeader(header), command))
    }

    private fun toEventHeader(commandHeader: CommandHeader): EventHeader {
        return EventHeader(
            eventType = commandHeader.msgType,
            source = commandHeader.source,
            sourceSequence = commandHeader.sourceSequence,
            globalSequence = 0L,
            eventTimeStampInMillis = commandHeader.timestampInMillis
        )
    }
}