package io.kofa.platform.codegen.domain.type

data class ArrayFieldTypeWrapper(
    val delegateType: DomainFieldType,
    override val fixedLength: Int? = null
): DomainFieldType {
    override val typeName: String get() = delegateType.typeName
    override val packageName: String get() = delegateType.packageName
    override val isArray: Boolean get() = true
    override val isEnum: Boolean get() = delegateType.isEnum
    override val isPrimitive: Boolean get() = delegateType.isPrimitive
    override val isBoolean: Boolean get() = delegateType.isBoolean
    override val isGenerated: Boolean get() = delegateType.isGenerated
    override val isComposite: Boolean get() = delegateType.isComposite && fixedLength != null
    override val sbeType: String? get() = delegateType.sbeType
}
