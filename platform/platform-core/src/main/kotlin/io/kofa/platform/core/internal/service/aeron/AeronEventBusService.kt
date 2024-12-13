package io.kofa.platform.core.internal.service.aeron

import io.aeron.FragmentAssembler
import io.aeron.Subscription
import io.aeron.logbuffer.FragmentHandler
import io.aeron.logbuffer.Header
import io.kofa.platform.api.util.EventDispatcher
import io.kofa.platform.core.internal.component.PlatformComponent
import io.kofa.platform.core.internal.service.EventBusService
import io.kofa.platform.core.internal.thread.eventloop.InvocationContext
import io.kofa.platform.core.internal.thread.eventloop.IterableTask
import org.agrona.DirectBuffer

internal class AeronEventBusService(private val subscription: Subscription, name: String = "AeronEventBus") :
    FragmentHandler, EventBusService, PlatformComponent, IterableTask(name) {
    private val assembler = FragmentAssembler(this)

    override fun id(): String {
        TODO("Not yet implemented")
    }

    override fun initialize() {
        TODO("Not yet implemented")
    }

    override suspend fun start() {
        TODO("Not yet implemented")
    }

    override suspend fun stop() {
        TODO("Not yet implemented")
    }

    override fun onFragment(
        buffer: DirectBuffer,
        offset: Int,
        length: Int,
        header: Header
    ) {
        buffer.getInt(offset)
        buffer.getStringAscii(offset)
        TODO("Not yet implemented")
    }

    override suspend fun poll(context: InvocationContext): Boolean {
        return subscription.poll(assembler, 1) != 0;
    }

    override fun addDispatcher(dispatcher: EventDispatcher<Any>) {
        TODO("Not yet implemented")
    }
}