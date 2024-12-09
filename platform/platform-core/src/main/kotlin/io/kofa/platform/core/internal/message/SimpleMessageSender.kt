package io.kofa.platform.core.internal.message

import arrow.atomic.AtomicLong
import io.kofa.platform.api.util.MessageSender
import io.kofa.platform.core.internal.service.CommandBusService
import io.kofa.platform.core.internal.service.CommandHeader
import io.kofa.platform.core.internal.service.meta.MessageMetaRegistry

internal class SimpleMessageSender(private val source: String, private val commandBusService: CommandBusService) :
    MessageSender<Any> {
    private val sourceSequencer = AtomicLong()

    override suspend fun send(message: Any) {
        commandBusService.publish(buildHeader(message), message)
    }

    private fun buildHeader(message: Any): CommandHeader {
        return CommandHeader(
            msgType = requireNotNull(MessageMetaRegistry.getMessageType(message::class)) { "unsupported message $message" },
            source = this.source,
            sourceSequence = sourceSequencer.incrementAndGet(),
            timestampInMillis = System.currentTimeMillis(),
        )
    }
}