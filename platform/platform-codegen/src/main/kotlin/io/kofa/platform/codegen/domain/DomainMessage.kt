package io.kofa.platform.codegen.domain

data class DomainMessage<F : DomainField>(val name: String, val fields: List<F>)
