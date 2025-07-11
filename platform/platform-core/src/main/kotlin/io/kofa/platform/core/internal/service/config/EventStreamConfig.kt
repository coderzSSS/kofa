package io.kofa.platform.core.internal.service.config

import io.kofa.platform.api.config.Config
import java.net.URI

internal data class EventStreamConfig(
    val publishUri: URI,
    val subscribeUri: URI,
    val mode: EventStreamMode = EventStreamMode.Auto,
    val config: Config
)

internal enum class EventStreamMode {
    Replay,
    Live,
    Auto
}

internal enum class MediaType(val protocol: String) {
    InProcess("ipc"),
    Kafka("kafka"),
    Aeron("aeron"),
    // ChronicleQueue
}