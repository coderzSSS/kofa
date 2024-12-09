package io.kofa.platform.core.internal.component.impl

import io.kofa.platform.api.dsl.model.ComponentDefinition
import io.kofa.platform.api.util.EventContext
import io.kofa.platform.api.util.EventDispatcher
import io.kofa.platform.core.internal.service.meta.MessageMetaRegistry

class UserDefinedEventDispatcher<T : Any>(
    private val componentDefinition: ComponentDefinition<T>
) : EventDispatcher<T> {
    override fun isInterested(eventType: Int): Boolean {
        return componentDefinition.eventDispatchers.any { dispatcher -> dispatcher.isInterested(eventType) } ||
                MessageMetaRegistry.getDomainClass(eventType)
                    ?.let { clazz -> componentDefinition.eventHandlers.containsKey(clazz) } == true
    }

    override suspend fun dispatch(eventType: Int, ctx: EventContext, event: T) {
        componentDefinition.eventDispatchers.filter { dispatcher -> dispatcher.isInterested(eventType) }
            .map { dispatcher -> dispatcher as EventDispatcher<T> }
            .forEach { dispatcher -> dispatcher.dispatch(eventType, ctx, event) }

        val domainClass = MessageMetaRegistry.getDomainClass(eventType)

        componentDefinition.eventHandlers[domainClass]?.invoke(ctx, event)
    }
}