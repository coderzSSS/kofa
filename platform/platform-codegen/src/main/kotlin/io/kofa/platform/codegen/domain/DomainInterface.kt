package io.kofa.platform.codegen.domain

data class DomainInterface<F : DomainField>(val name: String, val fields: List<F>)
