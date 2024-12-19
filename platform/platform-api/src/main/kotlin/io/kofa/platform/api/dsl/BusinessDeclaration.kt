package io.kofa.platform.api.dsl

import io.kofa.platform.api.dsl.builder.BusinessSpecBuilder
import io.kofa.platform.api.dsl.model.DomainHandlerDefinition
import io.kofa.platform.api.inject.InjectContext

abstract class BusinessDeclaration<T : Any>(private val declaration: BusinessDomainSpec<T>.() -> Unit) {
    fun getBusinessDeclaration(injectContextProviders: (String) -> Map<String, () -> InjectContext>): DomainHandlerDefinition<T> {
        val builder = BusinessSpecBuilder<T>()
        declaration.invoke(builder)

        val result = builder.build(injectContextProviders)

        check(result.isRight()) { result.leftOrNull() ?: "unknown error when building business declaration" }

        return result.getOrNull()!!
    }
}