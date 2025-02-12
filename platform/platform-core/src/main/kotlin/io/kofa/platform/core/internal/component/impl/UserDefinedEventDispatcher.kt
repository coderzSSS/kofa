package io.kofa.platform.core.internal.component.impl

import io.kofa.platform.api.util.EventContext
import io.kofa.platform.api.util.EventDispatcher
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

class UserDefinedEventDispatcher(
    private val eventHandlers: Map<KClass<*>, suspend EventContext.(Any) -> Unit>
) : EventDispatcher {
    override fun isInterested(eventType: KClass<*>): Boolean {
        return eventHandlers.keys.any { it.isSuperclassOf(eventType) }
    }

    override suspend fun <T> dispatch(ctx: EventContext, event: T) {
        event?.let {
            eventHandlers.filter { entry -> entry.key.isSuperclassOf(event::class) }.values.forEach { handler ->
                handler.invoke(ctx, event)
            }
        }
    }
}