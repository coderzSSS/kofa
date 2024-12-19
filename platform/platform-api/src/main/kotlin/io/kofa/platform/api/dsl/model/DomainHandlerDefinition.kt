package io.kofa.platform.api.dsl.model

data class DomainHandlerDefinition<T: Any>(
    val components: List<ComponentDefinition<T>>
)