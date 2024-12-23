package io.kofa.platform.core.internal.component.impl

import io.kofa.platform.api.dsl.model.ComponentDefinition
import io.kofa.platform.api.inject.ComponentModuleDeclaration
import io.kofa.platform.api.util.EventContext
import io.kofa.platform.api.util.EventDispatcher
import io.kofa.platform.core.internal.component.Component
import io.kofa.platform.core.internal.component.ComponentFactory
import io.kofa.platform.core.internal.component.config.ComponentConfig
import io.kofa.platform.core.internal.component.config.componentModule
import io.kofa.platform.core.internal.message.simpleMessageSenderModule
import org.koin.core.Koin
import kotlin.reflect.KClass

internal class DefaultComponentFactory(
    private val koin: Koin,
    private val globalEventDispatchers: List<EventDispatcher>,
    private val componentDefinitionProvider: (ComponentConfig) -> ComponentDefinition<*>?,
) : ComponentFactory {
    override fun create(componentConfig: ComponentConfig): Component {
        val componentId = componentConfig.type
        val definition =
            requireNotNull(componentDefinitionProvider.invoke(componentConfig)) { "no component definition found for id $componentId" }

        return doCreate(definition, componentConfig)
    }

    private fun doCreate(
        componentDefinition: ComponentDefinition<*>,
        componentConfig: ComponentConfig
    ): ScopedComponent {
        return MessageHandlerComponent(
            koin = koin,
            componentConfig = componentConfig,
            modules = componentDefinition.modules + buildDefaultComponentModules(componentConfig),
            eagerDispatchers = globalEventDispatchers + buildEventDispatcher(componentDefinition),
            lazyDispatchers = componentDefinition.eventDispatchers,
            stopAction = componentDefinition.stopAction,
            startAction = componentDefinition.startAction,
            errorHandler = componentDefinition.errorHandler,
        )
    }

    private fun buildDefaultComponentModules(config: ComponentConfig): List<ComponentModuleDeclaration> {
        return listOf(
            simpleMessageSenderModule(config),
            componentModule(config)
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun buildEventDispatcher(componentDefinition: ComponentDefinition<*>): EventDispatcher {
        return UserDefinedEventDispatcher(componentDefinition.eventHandlers as Map<KClass<*>, suspend EventContext.(Any) -> Unit>)
    }
}

