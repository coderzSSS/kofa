package io.kofa.platform.codegen.domain.type

import kotlin.reflect.KClass
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class JavaBuiltinType(
    override val typeName: String,
    val kClass: KClass<*>,
    override val sbeType: String? = null,
    override val isPrimitive: Boolean = true,
    override val isEnum: Boolean = false,
    override val isArray: Boolean = false,
    override val isGenerated: Boolean = false,
    override val fixedLength: Int? = null,
) : DomainFieldType {
    override val packageName: String get() = kClass.qualifiedName!!
    override val isBoolean: Boolean get() = kClass == Boolean::class
    override val isComposite get() = isPrimitive || (kClass == String::class && isFixed)
    override val isSbeType: Boolean get() = true

    fun alias(alias: String) = copy(typeName = alias)

    companion object {
        val BOOLEAN = JavaBuiltinType(
            typeName = "bool",
            kClass = Boolean::class,
            sbeType = "booleanType"
        )

        val BYTE = JavaBuiltinType(
            typeName = "byte",
            kClass = Byte::class,
            sbeType = "char"
        )

        val CHAR = JavaBuiltinType(
            typeName = "char",
            kClass = Short::class,
            sbeType = "char"
        )

        val SHORT = JavaBuiltinType(
            typeName = "short",
            kClass = Short::class,
            sbeType = "int16"
        )

        val INT = JavaBuiltinType(
            typeName = "int",
            kClass = Int::class,
            sbeType = "int32"
        )

        val LONG = JavaBuiltinType(
            typeName = "long",
            kClass = Long::class,
            sbeType = "int64"
        )

        val FLOAT = JavaBuiltinType(
            typeName = "float",
            kClass = Float::class,
            sbeType = "float"
        )

        val DOUBLE = JavaBuiltinType(
            typeName = "double",
            kClass = Double::class,
            sbeType = "double"
        )

        val STRING = JavaBuiltinType(
            typeName = "string",
            kClass = String::class,
            sbeType = "varStringEncoding",
            isPrimitive = false
        )

        val TIMESTAMP = LONG.alias("timestamp")

        @OptIn(ExperimentalUuidApi::class)
        val UUID = JavaBuiltinType(
            typeName = "uuid",
            kClass = Uuid::class,
            sbeType = "uuid_t",
            isPrimitive = false
        )

        val ALL = listOf(BOOLEAN, BYTE, CHAR, SHORT, INT, LONG, FLOAT, DOUBLE, STRING, UUID, TIMESTAMP)
    }
}
