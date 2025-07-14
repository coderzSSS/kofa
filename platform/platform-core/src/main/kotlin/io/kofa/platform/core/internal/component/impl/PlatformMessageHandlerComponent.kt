package io.kofa.platform.core.internal.component.impl

import io.kofa.platform.api.config.Config
import io.kofa.platform.api.inject.inject
import io.kofa.platform.api.logger.Logger
import io.kofa.platform.api.util.MessageSender
import io.kofa.platform.core.internal.component.PlatformComponent
import io.kofa.platform.core.internal.component.config.ComponentConfig
import io.kofa.platform.core.internal.component.config.componentModule
import io.kofa.platform.core.internal.message.simpleMessageSenderModule
import io.kofa.platform.message.PlatformMessage
import org.koin.core.Koin

const val PLATFORM_DOMAIN_NAME = "Platform"

internal abstract class PlatformMessageHandlerComponent(componentConfig: ComponentConfig, koin: Koin) :
    ScopedComponent(
        componentConfig,
        listOf(
            simpleMessageSenderModule(componentConfig, PLATFORM_DOMAIN_NAME),
            componentModule(componentConfig)
        ),
        koin
    ), PlatformComponent {
    constructor(componentType: String, componentConfig: Config, koin: Koin) : this(
        ComponentConfig(
            type = componentType,
            instanceName = componentType,
            config = componentConfig,
        ),
        koin = koin
    )

    protected val config: Config by inject()
    protected val messageSender: MessageSender<PlatformMessage> by inject()
    protected val logger: Logger by inject()
}