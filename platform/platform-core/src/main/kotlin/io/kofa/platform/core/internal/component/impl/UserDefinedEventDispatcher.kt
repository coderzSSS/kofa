package io.kofa.platform.core.internal.component.impl

import io.kofa.platform.api.util.EventContext
import io.kofa.platform.api.util.EventDispatcher
import kotlin.reflect.KClass

class UserDefinedEventDispatcher(
    private val eventHandlers: Map<KClass<*>, suspend EventContext.(Any) -> Unit>
) : EventDispatcher {
    override fun isInterested(eventType: KClass<*>): Boolean {
        return eventHandlers.contains(eventType)
    }

    override suspend fun <T> dispatch(ctx: EventContext, event: T) {
        event?.let {
            eventHandlers[it::class]?.invoke(ctx, event)
        }
    }
}