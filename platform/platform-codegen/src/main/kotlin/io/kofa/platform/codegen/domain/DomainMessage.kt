package io.kofa.platform.codegen.domain

data class DomainMessage<F : DomainField>(val id: Int? = null, val name: String, val fields: List<F>)
