package io.kofa.platform.core.internal.service.aeron

import io.aeron.FragmentAssembler
import io.aeron.Subscription
import io.aeron.archive.codecs.MessageHeaderDecoder
import io.aeron.logbuffer.FragmentHandler
import io.aeron.logbuffer.Header
import io.kofa.platform.api.codec.DirectBufferCodec
import io.kofa.platform.core.internal.service.common.AbstractEventBusService
import io.kofa.platform.core.internal.thread.eventloop.InvocationContext
import kotlinx.coroutines.runBlocking
import org.agrona.DirectBuffer

internal class AeronEventBusService(
    name: String = "AeronEventBus"
) : FragmentHandler, AbstractEventBusService(name) {
    val messageHeaderDecoder: MessageHeaderDecoder = MessageHeaderDecoder()

    private val assembler = FragmentAssembler(this)
    private lateinit var subscription: Subscription

    override suspend fun start() {
        TODO("Not yet implemented")
    }

    override suspend fun stop() {
        subscription.close()
    }

    private fun decode(codec: DirectBufferCodec) {

    }
    override fun onFragment(
        buffer: DirectBuffer,
        offset: Int,
        length: Int,
        header: Header
    ) {
        val eventProvider = { codec: DirectBufferCodec ->
            codec.decodeFromDirectBuffer<Any>(buffer, offset)
        }

        messageHeaderDecoder.wrap(buffer, offset)
        val templateId = messageHeaderDecoder.templateId()

        runBlocking {
            dispatch(templateId, eventProvider) {}
        }
    }

    override suspend fun poll(context: InvocationContext): Boolean {
        return subscription.poll(assembler, 1) > 0;
    }
}