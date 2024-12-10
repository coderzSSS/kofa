package io.kofa.platform.core.internal.component

import io.kofa.platform.api.dsl.BusinessDeclaration
import io.kofa.platform.api.dsl.model.ComponentDefinition
import io.kofa.platform.api.inject.InjectContext
import io.kofa.platform.core.internal.component.config.ComponentConfig
import io.kofa.platform.core.internal.component.config.source
import io.kofa.platform.core.internal.component.impl.DefaultComponentFactory
import io.kofa.platform.core.internal.component.impl.ScopedComponent
import org.koin.core.Koin
import java.util.*

internal class ComponentLoader(
    private val koin: Koin
) {
    private val componentConfigById = mutableMapOf<String, ComponentConfig>()
    private val componentMetaById = mutableMapOf<String, ComponentDefinition<*>>()
    private val componentById = mutableMapOf<String, ScopedComponent>()

    fun loadComponents(configList: Collection<ComponentConfig>): List<Component> {
        configList.forEach { config ->
            componentConfigById[config.source] = config
        }

        ServiceLoader.load(BusinessDeclaration::class.java).flatMap { businessDeclaration ->
            businessDeclaration.getBusinessDeclaration(this::getInjectContext).components
        }.forEach { componentDefinition ->
            val componentId = componentDefinition.id

            check(!componentMetaById.containsKey(componentId)) { "Component $componentId already defined." }
            componentMetaById[componentId] = componentDefinition
        }

        val componentFactory = DefaultComponentFactory(koin) { config -> componentMetaById[config.source] }

        componentConfigById.forEach { (componentId, _) ->
            val component =
                componentFactory.create(requireNotNull(componentConfigById[componentId]) { "no component config found for $componentId" })
            componentById[componentId] = component as ScopedComponent
        }

        return getLoadedComponents()
    }

    fun getLoadedComponents(): List<Component> = getSystemComponents() + componentById.values.toList()

    private fun getInjectContext(componentType: String): Map<String, () -> InjectContext> {
        return componentConfigById.values.filter { config -> config.type == componentType }.associateBy(
            { config -> config.source },
            { config -> { getComponentById(config.source) } }
        )
    }

    private fun getComponentById(componentId: String) =
        requireNotNull(componentById[componentId]) { "no component found for $componentId" }

    private fun getSystemComponents(): List<PlatformComponent> {
        return listOf()
    }
}