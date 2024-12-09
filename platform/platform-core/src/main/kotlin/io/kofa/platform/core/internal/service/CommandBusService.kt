package io.kofa.platform.core.internal.service

internal interface CommandBusService {
    suspend fun publish(header: CommandHeader, command: Any)
}

internal data class CommandHeader(
    val msgType: Int,
    val source: String,
    val sourceSequence: Long,
    val timestampInMillis: Long,
)