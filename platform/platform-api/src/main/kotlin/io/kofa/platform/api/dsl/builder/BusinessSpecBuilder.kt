package io.kofa.platform.api.dsl.builder

import arrow.core.Either
import arrow.core.mapOrAccumulate
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.kofa.platform.api.dsl.BusinessDomainSpec
import io.kofa.platform.api.dsl.ComponentSpec
import io.kofa.platform.api.dsl.model.DomainHandlerDefinition
import io.kofa.platform.api.inject.InjectContext

class BusinessSpecBuilder<E : Any> : BusinessDomainSpec<E> {
    private val components: MutableMap<String, ComponentSpec<E>.() -> Unit> = mutableMapOf()

    override fun component(
        name: String,
        spec: ComponentSpec<E>.() -> Unit
    ) {
        either {
            ensure(!components.containsKey(name)) {
                "component with name '$name' already exists."
            }

            components.putIfAbsent(name, spec)
        }.onLeft { error -> throw IllegalStateException(error) }
    }

    internal fun build(injectContextProvider: (String) -> InjectContext): Either<String, DomainHandlerDefinition> {
        return either {
            ensure(components.isNotEmpty()) { "no component specified" }
            val componentDefinitions = components.entries.mapOrAccumulate { (id, spec) ->
                val builder = ComponentSpecBuilder<E> { injectContextProvider.invoke(id) }
                spec.invoke(builder)
                builder.build(id).bind()
            }.mapLeft { list -> list.joinToString("\n") }

            DomainHandlerDefinition(
                componentDefinitions.bind()
            )
        }
    }
}