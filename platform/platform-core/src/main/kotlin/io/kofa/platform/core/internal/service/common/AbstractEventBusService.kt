package io.kofa.platform.core.internal.service.common

import io.kofa.platform.api.codec.DirectBufferCodec
import io.kofa.platform.api.util.EventContext
import io.kofa.platform.api.util.EventDispatcher
import io.kofa.platform.api.util.LazyEventDispatcher
import io.kofa.platform.core.internal.component.PlatformComponent
import io.kofa.platform.core.internal.service.EventBusService
import io.kofa.platform.core.internal.thread.eventloop.IterableTask

internal abstract class AbstractEventBusService(name: String) : EventBusService, PlatformComponent, IterableTask(name) {
    private val dispatchers = mutableListOf<EventDispatcher>()
    private val lazyDispatchers = mutableListOf<LazyEventDispatcher>()
    private val eventContext = MutableEventContext(0L)

    data class MutableEventContext(private var eventTimestampInMillis: Long) : EventContext {
        override val eventTimeInEpochMilli: Long get() = eventTimestampInMillis

        fun update(timestampInMillis: Long): MutableEventContext {
            eventTimestampInMillis = timestampInMillis
            return this;
        }

        fun reset() {
            eventTimestampInMillis = 0L
        }
    }

    override fun id(): String {
        return name
    }

    override fun initialize() {
    }

    override fun addDispatcher(dispatcher: EventDispatcher) {
        this.dispatchers.add(dispatcher)

        if (dispatcher is LazyEventDispatcher) {
            this.lazyDispatchers.add(dispatcher)
        }
    }

    override suspend fun start() {
    }

    override suspend fun stop() {
    }

    protected suspend fun dispatch(event: Any, action: MutableEventContext.() -> Unit) {
        if (dispatchers.isEmpty()) {
            return
        }

        eventContext.action()

        dispatchers.forEach {
            if (it.isInterested(event::class)) {
                it.dispatch(eventContext, event)
            }
        }

        eventContext.reset()
    }

    protected suspend fun dispatch(
        eventType: Int,
        eventProvider: (DirectBufferCodec) -> Any,
        action: MutableEventContext.() -> Unit
    ) {
        if (lazyDispatchers.isEmpty()) {
            return
        }
        eventContext.action()

        lazyDispatchers.forEach {
            if (it.isInterested(eventType)) {
                it.dispatch(eventContext, eventProvider)
            }
        }

        eventContext.reset()
    }
}