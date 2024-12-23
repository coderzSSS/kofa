package io.kofa.platform.core.internal.component.impl

import io.kofa.platform.api.dsl.model.ComponentDefinition
import io.kofa.platform.api.util.EventContext
import io.kofa.platform.api.util.EventDispatcher
import kotlin.reflect.KClass

class UserDefinedEventDispatcher(
    private val componentDefinition: ComponentDefinition<Any>
) : EventDispatcher {
    override fun isInterested(eventType: KClass<*>): Boolean {
        return componentDefinition.eventDispatchers.any { dispatcher -> dispatcher.value.isInterested(eventType) } ||
                componentDefinition.eventHandlers.contains(eventType)
    }

    override suspend fun <T> dispatch(ctx: EventContext, event: T) {
        componentDefinition.eventDispatchers.filter { dispatcher -> event != null && dispatcher.value.isInterested(event::class) }
            .forEach { dispatcher -> dispatcher.value.dispatch(ctx, event) }

        event?.let {
            componentDefinition.eventHandlers[it::class]?.invoke(ctx, event)
        }
    }
}