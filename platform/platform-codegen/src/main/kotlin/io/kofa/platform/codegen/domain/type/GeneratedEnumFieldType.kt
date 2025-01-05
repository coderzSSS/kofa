package io.kofa.platform.codegen.domain.type

import io.kofa.platform.codegen.domain.DomainEnumField

data class GeneratedEnumFieldType(
    override val typeName: String,
    override val packageName: String,
    val values: List<DomainEnumField>
): DomainFieldType {
    override val isComposite: Boolean get() = false
    override val isEnum: Boolean get() = true
    override val isArray: Boolean get() = false
    override val isPrimitive: Boolean get() = false
    override val isBoolean get() = false
    override val isGenerated get() = true
    override val isSbeType get() = false
}
