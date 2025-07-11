package io.kofa.platform.core.internal.service

import io.kofa.platform.api.message.EventHeader

internal interface CommandBusService {
    suspend fun publish(header: CommandHeader, command: Any)
}

internal data class CommandHeader(
    val msgType: Int,
    val source: String,
    val sourceSequence: Long,
    val timestampInMillis: Long,
) {
    fun toEventHeader(): EventHeader {
        return EventHeader(
            eventType = msgType,
            source = source,
            sourceSequence = sourceSequence,
            globalSequence = 0L,
            eventTimeStampInMillis = timestampInMillis
        )
    }
}