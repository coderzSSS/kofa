package io.kofa.platform.api.meta

data class MessageMeta(
    val msgType: Int,
    val version: Int,
    val domain: String
)