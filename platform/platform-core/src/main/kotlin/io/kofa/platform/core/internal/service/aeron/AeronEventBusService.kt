package io.kofa.platform.core.internal.service.aeron

import io.aeron.Aeron
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
    private val ctx: Aeron.Context,
    private val channel: String,
    private val sessionId: Int
) : FragmentHandler, AbstractEventBusService("Aeron-Evt-$channel-$sessionId") {
    private val assembler = FragmentAssembler(this)
    private lateinit var aeron: Aeron
    private lateinit var subscription: Subscription

    override fun initialize() {
        aeron = Aeron.connect(ctx)
    }

    override suspend fun start() {
        subscription = aeron.addSubscription(channel, sessionId)
    }

    override suspend fun stop() {
        if (this::subscription.isInitialized && this::aeron.isInitialized) {
            subscription.close()
            aeron.close()
        }

    }

    override fun onFragment(
        buffer: DirectBuffer,
        offset: Int,
        length: Int,
        header: Header
    ) {
        val result = codec.decodeFromDirectBuffer<Any>(buffer, offset)

        val eventProvider = { codec: DirectBufferCodec ->
            result.second
        }

        val templateId = result.first.eventType

        runBlocking {
            dispatch(templateId, eventProvider) {}
        }
    }

    override suspend fun poll(context: InvocationContext): Boolean {
        return subscription.poll(assembler, 1) > 0;
    }
}