package io.kofa.platform.codegen.domain

data class DomainType<F : DomainField>(val name: String, val fields: List<F>)
