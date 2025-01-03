package io.kofa.platform.codegen.domain.type

sealed interface DomainFieldType {
    val typeName: String
    val packageName: String
    val isArray: Boolean
    val isEnum: Boolean
    val isPrimitive: Boolean
    val isBoolean: Boolean
    val isGenerated: Boolean
    val isComposite: Boolean
    val isFixed: Boolean get() = fixedLength != null
    val fixedLength: Int?
    val isSbeType: Boolean get() = isEnum || isBoolean || isPrimitive || isComposite
    val sbeType: String?
}