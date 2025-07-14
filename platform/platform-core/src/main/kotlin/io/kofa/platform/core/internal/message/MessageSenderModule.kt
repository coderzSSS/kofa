package io.kofa.platform.core.internal.message

import io.kofa.platform.api.codec.CodecUtils
import io.kofa.platform.api.inject.ComponentModuleDeclaration
import io.kofa.platform.api.util.MessageSender
import io.kofa.platform.core.internal.component.config.ComponentConfig
import io.kofa.platform.core.internal.component.config.source
import org.koin.core.parameter.parametersOf
import org.koin.dsl.bind

internal fun simpleMessageSenderModule(config: ComponentConfig, domain: String? = null): ComponentModuleDeclaration = {
    scoped {
        SimpleMessageSender(
            CodecUtils.generateSourceAbbr(config.source),
            get(),
            get(),
            get(),
            get { parametersOf(domain) }
        )
    }.bind(MessageSender::class)
}