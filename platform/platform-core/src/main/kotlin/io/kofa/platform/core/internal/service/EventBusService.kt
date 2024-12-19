package io.kofa.platform.core.internal.service

import io.kofa.platform.api.util.EventDispatcher

internal interface EventBusService {
    fun addDispatcher(dispatcher: EventDispatcher)
}

internal data class EventHeader(
    var eventType: Int,
    var source: String,
    var sourceSequence: Long,
    var globalSequence: Long,
    var eventTimeStampInMillis: Long
)