package io.kofa.platform.codegen.domain.type

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class JavaBuiltinType(
    override val typeName: String,
    val javaClass: Class<*>,
    override val sbeType: String? = null,
    override val isGenerated: Boolean = false,
    override val fixedLength: Int? = null,
) : DomainFieldType {
    override val packageName: String get() = javaClass.`package`.name
    override val isArray: Boolean get() = javaClass.isArray
    override val isEnum: Boolean get() = javaClass.isEnum
    override val isPrimitive: Boolean get() = javaClass.isPrimitive
    override val isBoolean: Boolean get() = javaClass == Boolean.javaClass
    override val isComposite get() = isPrimitive || (javaClass == String.javaClass && isFixed)

    fun alias(alias: String) = copy(typeName = alias)

    companion object {
        val BOOLEAN = JavaBuiltinType(
            typeName = "bool",
            javaClass = Boolean.javaClass,
            sbeType = "booleanType"
        )

        val BYTE = JavaBuiltinType(
            typeName = "byte",
            javaClass = Byte.javaClass,
            sbeType = "char"
        )

        val CHAR = JavaBuiltinType(
            typeName = "char",
            javaClass = Short.javaClass,
            sbeType = "char"
        )

       val SHORT = JavaBuiltinType(
            typeName = "short",
            javaClass = Short.javaClass,
            sbeType = "int16"
        )

       val INT = JavaBuiltinType(
            typeName = "int",
            javaClass = Int.javaClass,
            sbeType = "int32"
        )

        val LONG = JavaBuiltinType(
            typeName = "long",
            javaClass = Long.javaClass,
            sbeType = "int64"
        )

        val FLOAT = JavaBuiltinType(
            typeName = "float",
            javaClass = Float.javaClass,
            sbeType = "float"
        )

        val DOUBLE = JavaBuiltinType(
            typeName = "double",
            javaClass = Double.javaClass,
            sbeType = "double"
        )

        val STRING = JavaBuiltinType(
            typeName = "string",
            javaClass = String.javaClass,
            sbeType = "varStringEncoding"
        )

        val TIMESTAMP = LONG.alias("timestamp")

        @OptIn(ExperimentalUuidApi::class)
        val UUID = JavaBuiltinType(
            typeName = "uuid",
            javaClass = Uuid.javaClass,
            sbeType = "uuid_t"
        )

        val ALL = listOf(BOOLEAN, BYTE, CHAR, SHORT, INT, LONG, FLOAT, DOUBLE, STRING, UUID, TIMESTAMP)
    }
}
