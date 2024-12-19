package io.kofa.platform.api.util;

import kotlin.reflect.KClass

interface EventDispatcher {
    fun isInterested(eventType: KClass<*>): Boolean

    suspend fun <T> dispatch(ctx: EventContext, event: T)
}
