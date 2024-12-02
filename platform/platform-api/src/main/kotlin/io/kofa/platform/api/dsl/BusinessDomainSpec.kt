package io.kofa.platform.api.dsl

import io.kofa.platform.api.annotation.KofaDsl

@KofaDsl
interface BusinessDomainSpec<E : Any> {
    fun component(name: String, spec: ComponentSpec<E>.() -> Unit)
}