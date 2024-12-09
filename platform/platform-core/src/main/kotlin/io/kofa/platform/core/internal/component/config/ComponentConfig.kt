package io.kofa.platform.core.internal.component.config

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kofa.platform.api.config.Config
import io.kofa.platform.api.logger.Logger

internal data class ComponentConfig(
    val type: String,
    val instanceName: String,
    val allowMultipleInstance: Boolean = false,
    val readOnly: Boolean = false,

    // origin config
    val config: Config
)

internal val ComponentConfig.source: String
    get() {
        return if (allowMultipleInstance) {
            "$type[$instanceName]"
        } else {
            type
        }
    }

internal val ComponentConfig.logger: Logger
    get() {
        return KotlinLogging.logger(source)
    }

