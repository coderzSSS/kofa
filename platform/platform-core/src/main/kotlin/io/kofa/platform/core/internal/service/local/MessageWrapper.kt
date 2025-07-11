package io.kofa.platform.core.internal.service.local

import io.kofa.platform.api.message.EventHeader


internal data class MessageWrapper(
    val header: EventHeader,
    val payload: Any,
)