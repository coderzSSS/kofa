package io.kofa.platform.core.internal.service.aeron.config

data class AeronConfig(
    val deleteDirOnStart: Boolean,
    val enableArchive: Boolean,
    val channel: String
)