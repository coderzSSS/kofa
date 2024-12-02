package io.kofa.platform.api.dsl

import io.kofa.platform.api.dsl.builder.DomainSpecBuilder
import io.kofa.platform.api.dsl.model.DomainMetaDefinition

abstract class DomainDeclaration(private val declaration: DomainSpec.() -> Unit) {
    fun getDomainDeclaration():DomainMetaDefinition {
        val builder = DomainSpecBuilder()
        declaration.invoke(builder)

        return builder.build()
    }
}
