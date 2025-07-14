package io.kofa.platform.core.internal.service.local

import io.kofa.platform.core.internal.media.LocalEventBus
import io.kofa.platform.core.internal.service.CommandBusService
import io.kofa.platform.core.internal.service.CommandHeader

internal class LocalCommandBusService(private val localBus: LocalEventBus<MessageWrapper>) : CommandBusService {
    override suspend fun publish(header: CommandHeader, command: Any): Boolean {
        localBus.publish(MessageWrapper(header.toEventHeader(), command))
        return true
    }
}