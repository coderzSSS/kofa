package io.kofa.platform.core.internal.service.aeron.config

data class AeronConfig(
    val sessionId: Int,
    val channel: String? = null
)