package io.kofa.platform.core.internal.message

import io.kofa.platform.api.inject.ComponentModuleDeclaration
import io.kofa.platform.api.util.MessageSender
import io.kofa.platform.core.internal.component.config.ComponentConfig
import io.kofa.platform.core.internal.component.config.source
import org.koin.dsl.bind

internal fun simpleMessageSenderModule(config: ComponentConfig): ComponentModuleDeclaration = {
    scoped {
        SimpleMessageSender(config.source, get())
    }.bind(MessageSender::class)
}