package io.kofa.platform.core.internal.service.aeron

import io.aeron.FragmentAssembler
import io.aeron.Subscription
import io.aeron.logbuffer.FragmentHandler
import io.aeron.logbuffer.Header
import io.kofa.platform.api.codec.DirectBufferCodec
import io.kofa.platform.core.internal.service.common.AbstractEventBusService
import io.kofa.platform.core.internal.thread.eventloop.InvocationContext
import kotlinx.coroutines.runBlocking
import org.agrona.DirectBuffer

internal class AeronEventBusService(
    private val codec: DirectBufferCodec,
    name: String = "AeronEventBus"
) : FragmentHandler, AbstractEventBusService(name) {
    private val assembler = FragmentAssembler(this)
    private lateinit var subscription: Subscription

    override suspend fun start() {
        TODO("Not yet implemented")
    }

    override suspend fun stop() {
        subscription.close()
    }

    override fun onFragment(
        buffer: DirectBuffer,
        offset: Int,
        length: Int,
        header: Header
    ) {
        val event = codec.decodeFromDirectBuffer<Any>(buffer, offset)

        runBlocking {
            dispatch(event) {}
        }
    }

    override suspend fun poll(context: InvocationContext): Boolean {
        return subscription.poll(assembler, 1) > 0;
    }
}