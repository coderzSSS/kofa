package io.kofa.platform.codegen.domain

import io.kofa.platform.codegen.domain.type.DomainFieldType

sealed interface DomainField {
    val name: String
    val deprecated: Boolean
}

data class ResolvedDomainField(val id: Int?, override val name: String, val type: DomainFieldType,
                               override val deprecated: Boolean = false) : DomainField

data class PlainDomainField(val id: Int? = null, override val name: String, val typeName: String, override val deprecated: Boolean = false, val length: Int? = null) : DomainField

data class DomainEnumField(override val name: String, val value: Int? = null, override val deprecated: Boolean = false) : DomainField
