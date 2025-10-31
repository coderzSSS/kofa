package io.kofa.platform.core.internal.message

import arrow.atomic.AtomicLong
import io.kofa.platform.api.logger.Logger
import io.kofa.platform.api.util.EventContext
import io.kofa.platform.api.util.EventDispatcher
import io.kofa.platform.api.util.MessageSender
import io.kofa.platform.core.internal.service.CommandBusService
import io.kofa.platform.core.internal.service.CommandHeader
import io.kofa.platform.core.internal.service.EventBusService
import io.kofa.platform.core.internal.service.meta.MessageMetaRegistry
import io.kofa.platform.core.internal.thread.eventloop.EventLoop
import io.kofa.platform.core.internal.thread.eventloop.InvocationContext
import io.kofa.platform.core.internal.thread.eventloop.IterableTask
import java.util.concurrent.LinkedBlockingQueue
import kotlin.reflect.KClass

internal class SimpleMessageSender(
    private val source: String,
    private val logger: Logger,
    eventLoop: EventLoop,
    eventBusService: EventBusService,
    private val commandBusService: CommandBusService
) :
    MessageSender<Any>, EventDispatcher, IterableTask("message-sender-$source") {
    private val sourceSequencer = AtomicLong()
    private val retryQueue = LinkedBlockingQueue<Any>()

    init {
        eventBusService.addDispatcher(this)
        eventLoop.addTask(this)
    }

    override fun isInterested(eventType: KClass<*>): Boolean {
        return true
    }

    override suspend fun <T> dispatch(ctx: EventContext, event: T) {
        if (ctx.source == this.source) {
            sourceSequencer.set(ctx.sourceSequence)
        }
    }

    override suspend fun poll(context: InvocationContext): Boolean {
        retryQueue.peek()?.let {
            send(it)
        }

        return !retryQueue.isEmpty()
    }

    override suspend fun send(message: Any) {
        val success = doSend(message)
        if (!success) {
            logger.warn { "failed to send message $message" }
            retryQueue.offer(message)
        }
    }

    private suspend fun doSend(message: Any): Boolean {
        return commandBusService.publish(buildHeader(message), message)
    }

    private fun buildHeader(message: Any): CommandHeader {
        return CommandHeader(
            msgType = requireNotNull(MessageMetaRegistry.getMessageType(message::class)) { "unsupported message $message" },
            source = this.source,
            sourceSequence = sourceSequencer.incrementAndGet(),
            timestampInMillis = System.currentTimeMillis(),
        )
    }

    override fun toString(): String {
        return "SimpleMessageSender(source='$source', sourceSequence=$sourceSequencer, pendingRetries=${retryQueue.size})"
    }
}