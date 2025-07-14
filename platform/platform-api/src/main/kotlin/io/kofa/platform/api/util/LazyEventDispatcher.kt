package io.kofa.platform.api.util

import io.kofa.platform.api.codec.DirectBufferCodec
import io.kofa.platform.api.message.EventHeader

interface LazyEventDispatcher {
    suspend fun <T> dispatch(ctx: EventContext, eventProvider: (DirectBufferCodec) -> Pair<EventHeader, T>)
}