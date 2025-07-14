package io.kofa.platform.core.internal.service.aeron

import io.aeron.Aeron
import io.aeron.FragmentAssembler
import io.aeron.Subscription
import io.aeron.logbuffer.FragmentHandler
import io.aeron.logbuffer.Header
import io.kofa.platform.api.codec.CodecUtils
import io.kofa.platform.api.codec.DirectBufferCodec
import io.kofa.platform.core.internal.service.common.AbstractEventBusService
import io.kofa.platform.core.internal.thread.eventloop.InvocationContext
import io.kofa.platform.message.sbe.MessageHeaderDecoder
import kotlinx.coroutines.runBlocking
import org.agrona.DirectBuffer

internal class AeronEventBusService(
    private val aeron: Aeron,
    private val channel: String,
    private val sessionId: Int
) : FragmentHandler, AbstractEventBusService("Aeron-Evt-$channel-$sessionId") {
    private val assembler = FragmentAssembler(this)
    private lateinit var subscription: Subscription

    private val messageHeaderDecoder = MessageHeaderDecoder()

    override fun initialize() {
    }

    override suspend fun start() {
        subscription = aeron.addSubscription(channel, sessionId)
    }

    override suspend fun stop() {
        if (this::subscription.isInitialized ) {
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
        messageHeaderDecoder.wrap(buffer, offset)
        val source = CodecUtils.decodeIntToSource(messageHeaderDecoder.source())
        val ts = messageHeaderDecoder.eventTimestamp()
        val eventType = messageHeaderDecoder.templateId()
        val sourceSeq = messageHeaderDecoder.sourceSequence()
        val globalSeq = messageHeaderDecoder.globalSequence()

        val eventProvider = { codec: DirectBufferCodec ->
            codec.decodeFromDirectBuffer<Any>(buffer, offset)
        }

        runBlocking {
            dispatch(eventProvider) {
                this.eventType = eventType
                this.sourceSequence = sourceSeq
                this.globalSequence = globalSeq
                this.source = source
                this.sourceSequence = ts
                update(ts)
            }
        }
    }

    override suspend fun poll(context: InvocationContext): Boolean {
        return subscription.poll(assembler, 1) > 0;
    }
}