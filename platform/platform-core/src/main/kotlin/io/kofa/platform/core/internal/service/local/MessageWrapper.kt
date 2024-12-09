package io.kofa.platform.core.internal.service.local

import io.kofa.platform.core.internal.service.EventHeader

internal data class MessageWrapper(
    val header: EventHeader,
    val payload: Any,
)