package io.kofa.platform.core.internal.service.meta

internal data class MessageMeta(
    val msgType: Int,
    val version: Int,
    val domain: String
)
