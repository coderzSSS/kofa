package io.kofa.platform.codegen.writer.kotlin

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.kofa.platform.codegen.domain.*
import io.kofa.platform.codegen.domain.type.*
import kotlin.reflect.KClass

object KotlinGeneratorUtils {
    fun ResolvedDomain.qualifiedName(): String {
        return "${pkgName}.${domainName}"
    }

    fun ResolvedDomain.eventClassName(name: String) = ClassName(pkgName, resolveTypeClassName(name, true, false))

    fun ResolvedDomain.messageClassName(name: String) = ClassName(pkgName, resolveTypeClassName(name, true, true))

    fun ResolvedDomain.messageHandlerClassName() = ClassName(pkgName, domainName.cap() + "MessageHandler")

    fun ResolvedDomain.messageConstantsClassName() = ClassName(pkgName, domainName.cap() + "MessageConstants")

    fun ResolvedDomain.messageCodecClassName() = ClassName(pkgName, domainName.cap() + "MessageCodec")

    fun ResolvedDomain.domainDeclarationClassName() = ClassName(pkgName, domainName.cap() + "DomainDeclaration")

    fun ResolvedDomain.businessDeclarationClassName() = ClassName(pkgName, domainName.cap() + "BusinessDeclaration")

    fun ResolvedDomain.simpleClassName(name: String) = ClassName(pkgName, name.cap())

    fun ResolvedDomain.generatedClassName(fieldType: DomainFieldType, mutable: Boolean = true): ClassName {
        return when (fieldType) {
            is GeneratedFieldType -> {
                return ClassName(pkgName, resolveTypeClassName(fieldType.typeName, fieldType.isMessage, mutable))
            }

            is GeneratedEnumFieldType -> simpleClassName(fieldType.typeName)

            is JavaBuiltinType -> fieldType.kClass.asClassName()

            else -> throw IllegalStateException("Unsupported field type $fieldType")
        }
    }

    fun ResolvedDomain.sbeClassName(name: String) = ClassName("$pkgName.sbe", name.cap())

    fun ResolvedDomain.sealedDomainMessageClassName() = getSealedDomainMessageClassName(this)

    fun getSealedDomainMessageClassName(domain: ResolvedDomain) =
        ClassName(domain.pkgName, getSealedDomainMessageName(domain.domainName))

    fun getSealedDomainMessageName(domainName: String) = domainName.cap() + "Message"

    fun isNeedFlatten(fieldType: DomainFieldType): Boolean {
        return fieldType is ArrayFieldTypeWrapper || (fieldType is GeneratedFieldType && !fieldType.fields.all { e -> e.value.isPrimitive })
    }

    fun resolveTypeName(
        domain: ResolvedDomain,
        fieldType: DomainFieldType,
        mutable: Boolean,
        nullable: Boolean = fieldType.isNullable
    ): TypeName {
        val listClassName = if (mutable) {
            MUTABLE_LIST
        } else {
            LIST
        }

        val isMessage = !isDomainType(domain, fieldType.typeName)

        val typeName = when (fieldType) {
            is JavaBuiltinType -> fieldType.kClass.asTypeName()
            is GeneratedFieldType -> ClassName(
                domain.pkgName,
                resolveTypeClassName(fieldType.typeName, isMessage, mutable)
            )

            is GeneratedEnumFieldType -> ClassName(
                domain.pkgName,
                fieldType.typeName
            )

            is ArrayFieldTypeWrapper -> listClassName.parameterizedBy(
                resolveTypeName(
                    domain,
                    fieldType = fieldType.delegateType,
                    mutable = false,
                    nullable = false
                )
            )
        }

        return if (fieldType.isArray || fieldType.isPrimitive || fieldType.isEnum || fieldType.isBoolean) {
            typeName.copy(nullable = false)
        } else {
            typeName.copy(nullable = nullable)
        }
    }

    fun isDomainType(domain: ResolvedDomain, typeName: String): Boolean {
        return domain.types.any { type -> type.name == typeName }
    }

    fun resolveTypeClassName(typeName: String, isMessage: Boolean, mutable: Boolean): String {
        val typeName = typeName.cap()
        return if (isMessage) {
            if (mutable) {
                typeName + "Message"
            } else {
                typeName + "Event"
            }
        } else {
            if (mutable) {
                "Mutable$typeName"
            } else {
                typeName
            }
        }
    }

    fun DomainMessage<*>.metaPropertyName() = "META_${name.uppercase()}"

    fun DomainMessage<*>.messageClassName(domain: ResolvedDomain) =
        ClassName(domain.pkgName, resolveTypeClassName(name, true, true))

    fun DomainMessage<*>.eventClassName(domain: ResolvedDomain) =
        ClassName(domain.pkgName, resolveTypeClassName(name, true, false))

    fun DomainMessage<*>.eventHandlerName(): String {
        return "on" + name.cap() + "Event"
    }

    fun String.cap() = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

    fun String.deCap() = replaceFirstChar { if (it.isUpperCase()) it.lowercase() else it.toString() }

    fun KClass<*>.star() = asClassName().parameterizedBy(STAR)

    fun flattenFieldName(vararg name: String): String {
        return buildString {
            val iterator = name.iterator()
            append(iterator.next())
            while (iterator.hasNext()) {
                append(iterator.next().cap())
            }
        }
    }

    fun ResolvedDomain.findEnumByName(name: String): DomainType<DomainEnumField>? {
        val result = this.enums.find { it.name == name }
        if (result == null) {
            for (domain in this.imports) {
                val r = domain.findEnumByName(name)
                if (r != null) {
                    return r
                }
            }
        }

        return result
    }

    fun ResolvedDomain.findTypeByName(name: String): DomainType<ResolvedDomainField>? {
        val result = this.types.find { it.name == name }
        if (result == null) {
            for (domain in this.imports) {
                val r = domain.findTypeByName(name)
                if (r != null) {
                    return r
                }
            }
        }

        return result
    }

    fun ResolvedDomain.findMessageByName(name: String): DomainMessage<ResolvedDomainField>? {
        val result = this.messages.find { it.name == name }
        if (result == null) {
            for (domain in this.imports) {
                val r = domain.findMessageByName(name)
                if (r != null) {
                    return r
                }
            }
        }

        return result
    }

    fun ResolvedDomain.sbeBooleanType() = sbeClassName("BooleanType")
}