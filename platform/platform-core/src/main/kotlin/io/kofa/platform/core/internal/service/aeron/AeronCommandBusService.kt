package io.kofa.platform.core.internal.service.aeron

import io.aeron.Aeron
import io.aeron.Publication
import io.kofa.platform.api.codec.DirectBufferCodec
import io.kofa.platform.core.internal.component.PlatformComponent
import io.kofa.platform.core.internal.service.CommandBusService
import io.kofa.platform.core.internal.service.CommandHeader
import org.agrona.ExpandableDirectByteBuffer
import org.agrona.MutableDirectBuffer

internal class AeronCommandBusService(
    private val codec: DirectBufferCodec,
    private val ctx: Aeron.Context,
    private val channel: String,
    private val session: Int
) : CommandBusService, PlatformComponent {
    private lateinit var publication: Publication
    private lateinit var aeron: Aeron

    private val buffer: MutableDirectBuffer = ExpandableDirectByteBuffer()

    override fun id(): String {
        return "Aeron-Cmd-$channel-$session"
    }

    override fun initialize() {
        aeron = Aeron.connect(ctx)
    }

    override suspend fun start() {
        publication = aeron.addPublication(channel, session)
    }

    override suspend fun stop() {
        publication.close()
        aeron.close()
    }

    override suspend fun publish(header: CommandHeader, command: Any) {
        val length = codec.encodeToDirectBuffer(header.toEventHeader(),command, buffer, 0)
        publication.offer(buffer, 0, length)
    }
}