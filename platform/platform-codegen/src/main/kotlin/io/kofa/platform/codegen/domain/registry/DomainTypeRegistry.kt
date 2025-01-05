package io.kofa.platform.codegen.domain.registry

import io.kofa.platform.codegen.domain.type.DomainFieldType
import io.kofa.platform.codegen.domain.type.JavaBuiltinType

class DomainTypeRegistry {
    companion object {
        private val buildInJavaTypes = buildMap<String, JavaBuiltinType> {
            JavaBuiltinType.ALL.forEach { t -> put(t.typeName.uppercase(), t) }
        }
    }

    private val customTypes = mutableMapOf<String, DomainFieldType>()

    fun register(type: DomainFieldType) {
        check(customTypes.putIfAbsent(type.typeName, type) == null) { "type $type is already registered" }
    }

    fun tryGet(typeName: String): DomainFieldType? {
        return buildInJavaTypes[typeName.uppercase()] ?: customTypes[typeName]
    }

    fun get(typeName: String) = requireNotNull(tryGet(typeName)) { "type $typeName not found" }
}