package io.kofa.platform.core.internal.component

import io.kofa.platform.api.dsl.BusinessDeclaration
import io.kofa.platform.api.dsl.model.ComponentDefinition
import io.kofa.platform.core.internal.component.impl.ScopedComponent
import java.util.*

internal class ComponentLoader(private val componentFactory: ComponentFactory) {
    private val componentMetaById = mutableMapOf<String, ComponentDefinition<*>>()
    private val componentById = mutableMapOf<String, ScopedComponent>()

    fun loadComponents() {
        ServiceLoader.load(BusinessDeclaration::class.java).flatMap { businessDeclaration ->
            businessDeclaration.getBusinessDeclaration(this::getInjectContext).components
        }.forEach { componentDefinition ->
            check(!componentMetaById.containsKey(componentDefinition.name)) { "Component ${componentDefinition.name} already loaded." }

            componentMetaById[componentDefinition.name] = componentDefinition
            componentById[componentDefinition.name] = createScopedComponent(componentDefinition)
        }
    }

    private fun createScopedComponent(definition: ComponentDefinition<*>): ScopedComponent {
        return componentFactory.create(definition.name) as ScopedComponent
    }

    private fun getInjectContext(componentId: String) =  componentById[componentId]!!
}