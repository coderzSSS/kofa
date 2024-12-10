package io.kofa.platform.core.internal.service.local

import io.kofa.platform.api.util.EventContext
import io.kofa.platform.api.util.EventDispatcher
import io.kofa.platform.core.internal.component.PlatformComponent
import io.kofa.platform.core.internal.media.LocalEventBus
import io.kofa.platform.core.internal.service.EventBusService
import io.kofa.platform.core.internal.thread.eventloop.EventLoop
import io.kofa.platform.core.internal.thread.eventloop.InvocationContext
import io.kofa.platform.core.internal.thread.eventloop.IterableTask

internal class LocalEventBusService(
    val eventLoop: EventLoop,
    val localBus: LocalEventBus<MessageWrapper>,
    name: String
) : EventBusService, PlatformComponent, IterableTask(name) {
    private val dispatchers = mutableListOf<EventDispatcher<Any>>()
    private val eventContext = MutableEventContext(0L)

    data class MutableEventContext(private var eventTimestampInMillis: Long) : EventContext {
        override val eventTimeInEpochMilli: Long get() = eventTimestampInMillis

        fun update(timestampInMillis: Long): MutableEventContext {
            eventTimestampInMillis = timestampInMillis
            return this;
        }
    }

    override fun id(): String {
        return name
    }

    override fun initialize() {
    }

    override suspend fun start() {
        localBus.subscribe { eventWrapper ->
            val header = eventWrapper.header

            dispatchers.forEach { dispatcher ->
                dispatcher.dispatch(
                    header.eventType,
                    eventContext.update(header.eventTimeStampInMillis),
                    eventWrapper.payload
                )
            }
        }
    }

    override suspend fun stop() {
        localBus.close()
    }

    override suspend fun poll(context: InvocationContext): Boolean {
        // keep event loop running
        return true
    }

    override fun addDispatcher(dispatcher: EventDispatcher<Any>) {
        this.dispatchers.add(dispatcher)
//        localBus.subscribe { eventWrapper ->
//            val header = eventWrapper.header
//            if (globalSequence < header.globalSequence) {
//                globalSequence = header.globalSequence
//            } else {
//                header.globalSequence = ++globalSequence
//            }
//
//            dispatcher.dispatch(
//                header.eventType,
//                eventContext.update(header.eventTimeStampInMillis),
//                eventWrapper.payload
//            )
//        }
    }
}