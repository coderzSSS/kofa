package io.kofa.platform.core.internal.service.aeron

import io.aeron.Aeron
import io.aeron.Publication
import io.kofa.platform.api.codec.DirectBufferCodec
import io.kofa.platform.api.logger.logger
import io.kofa.platform.core.internal.component.PlatformComponent
import io.kofa.platform.core.internal.service.CommandBusService
import io.kofa.platform.core.internal.service.CommandHeader
import io.kofa.platform.core.internal.thread.eventloop.EventLoop
import kotlinx.coroutines.runBlocking
import org.agrona.ExpandableDirectByteBuffer
import org.agrona.MutableDirectBuffer

internal class AeronCommandBusService(
    eventLoop: EventLoop,
    private val codec: DirectBufferCodec,
    private val aeron: Aeron,
    private val channel: String,
    private val session: Int
) : CommandBusService, PlatformComponent {
    private lateinit var publication: Publication

    private val buffer: MutableDirectBuffer = ExpandableDirectByteBuffer()

    init {
        initialize()

        runBlocking {
            logger.info { "Initializing command bus service: ${id()}" }
            start()
        }

        eventLoop.onShutdownExecute("Component[${id()}]-Stop") {
            stop()
        }
    }

    override fun id(): String {
        return "Aeron-Cmd-$channel-$session"
    }

    override fun initialize() {
    }

    override suspend fun start() {
        publication = aeron.addPublication(channel, session)
    }

    override suspend fun stop() {
        if (this::publication.isInitialized) {
            publication.close()
            aeron.close()
        }
    }

    override suspend fun publish(header: CommandHeader, command: Any): Boolean {
        val length = codec.encodeToDirectBuffer(header.toEventHeader(),command, buffer, 0)
        val result = publication.offer(buffer, 0, length)
        return result >= 0
    }
}