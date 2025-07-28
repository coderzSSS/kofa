package io.kofa.platform.codegen.domain.type

import kotlin.reflect.KClass

data class GeneratedFieldType(
    override val typeName: String,
    override val packageName: String,
    override val isEnum: Boolean,
    override val isComposite: Boolean,
    val isMessage: Boolean,
    val fields: Map<String, DomainFieldType>,
    val originClass: KClass<*>? = null
) : DomainFieldType {
    override val isArray: Boolean get() = false
    override val isPrimitive: Boolean get() = false
    override val isBoolean get() = false
    override val isGenerated get() = true
}
