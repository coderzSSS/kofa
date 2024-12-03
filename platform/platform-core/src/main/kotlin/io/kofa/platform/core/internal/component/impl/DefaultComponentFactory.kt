package io.kofa.platform.core.internal.component.impl

import io.kofa.platform.api.dsl.model.ComponentDefinition
import io.kofa.platform.api.util.EventDispatcher
import io.kofa.platform.core.internal.component.Component
import io.kofa.platform.core.internal.component.ComponentConfig
import io.kofa.platform.core.internal.component.ComponentFactory
import org.koin.core.Koin

internal class DefaultComponentFactory<T : Any>(
    private val koin: Koin,
    private val componentDefinitionProvider: (ComponentConfig) -> ComponentDefinition<T>?,
) : ComponentFactory {
    override fun create(componentConfig: ComponentConfig): Component {
        val componentId = componentConfig.type
        val definition = requireNotNull(componentDefinitionProvider.invoke(componentConfig)) { "no component definition found for id $componentId" }

        return doCreate(definition, componentConfig)
    }

    private fun doCreate(componentDefinition: ComponentDefinition<T>, componentConfig: ComponentConfig): ScopedComponent {
        return MessageHandlerComponent<T>(
            koin = koin,
            componentConfig = componentConfig,
            modules = componentDefinition.modules,
            listOf(buildEventDispatcher(componentDefinition)),
            stopAction = componentDefinition.stopAction,
            startAction = componentDefinition.startAction,
            errorHandler = componentDefinition.errorHandler,
        )
    }

    private fun buildEventDispatcher(componentDefinition: ComponentDefinition<T>): EventDispatcher<T> {
        return UserDefinedEventDispatcher(componentDefinition)
    }
}

