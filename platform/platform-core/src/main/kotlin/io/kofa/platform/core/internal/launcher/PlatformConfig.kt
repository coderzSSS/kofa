package io.kofa.platform.core.internal.launcher

import io.kofa.platform.core.internal.component.config.ComponentConfig
import io.kofa.platform.core.internal.service.config.EventStreamConfig
import io.kofa.platform.core.internal.thread.eventloop.config.EventLoopConfig
import kotlinx.datetime.TimeZone

internal data class PlatformConfig(
    val domain: String,
    val timezone: TimeZone = TimeZone.currentSystemDefault(),
    val eventLoop: EventLoopConfig,
    val eventStream: EventStreamConfig,
    val components: List<ComponentConfig>
)