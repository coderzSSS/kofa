package io.kofa.platform.codegen.domain.type

data class ArrayFieldTypeWrapper(
    val delegateType: DomainFieldType,
    override val fixedLength: Int? = null
): DomainFieldType {
    override val typeName: String get() = delegateType.typeName
    override val packageName: String get() = delegateType.packageName
    override val isArray: Boolean get() = true
    override val isEnum: Boolean get() = false
    override val isPrimitive: Boolean get() = false
    override val isBoolean: Boolean get() = false
    override val isGenerated: Boolean get() = delegateType.isGenerated
    override val isComposite: Boolean get() = false
}
