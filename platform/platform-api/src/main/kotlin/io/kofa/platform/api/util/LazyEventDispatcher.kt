package io.kofa.platform.api.util

import io.kofa.platform.api.codec.DirectBufferCodec

interface LazyEventDispatcher {
    fun isInterested(eventType: Int): Boolean

    suspend fun <T> dispatch(ctx: EventContext, eventProvider: (DirectBufferCodec) -> T)
}