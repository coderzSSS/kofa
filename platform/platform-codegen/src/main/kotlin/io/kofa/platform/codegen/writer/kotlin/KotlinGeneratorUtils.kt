package io.kofa.platform.codegen.writer.kotlin

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.kofa.platform.codegen.domain.DomainMessage
import io.kofa.platform.codegen.domain.ResolvedDomain
import io.kofa.platform.codegen.domain.type.*
import kotlin.reflect.KClass

object KotlinGeneratorUtils {
    fun ResolvedDomain.messageHandlerClassName() = ClassName(pkgName, domainName + "MessageHandler")

    fun ResolvedDomain.messageConstantsClassName() = ClassName(pkgName, domainName + "MessageConstants")

    fun ResolvedDomain.messageCodecClassName() = ClassName(pkgName, domainName + "MessageCodec")

    fun ResolvedDomain.domainDeclarationClassName() = ClassName(pkgName, domainName + "DomainDeclaration")

    fun ResolvedDomain.businessDeclarationClassName() = ClassName(pkgName, domainName + "BusinessDeclaration")

    fun ResolvedDomain.sealedDomainMessageClassName() = getSealedDomainMessageClassName(this)

    fun getSealedDomainMessageClassName(domain: ResolvedDomain) =
        ClassName(domain.pkgName, getSealedDomainMessageName(domain.domainName))

    fun getSealedDomainMessageName(domainName: String) = domainName + "Message"

    fun resolveTypeName(
        fieldType: DomainFieldType,
        isMessage: Boolean,
        mutable: Boolean,
        nullable: Boolean
    ): TypeName {
        val listClassName = if (mutable) {
            MUTABLE_LIST
        } else {
            LIST
        }

        val typeName = when (fieldType) {
            is JavaBuiltinType -> fieldType.javaClass.asTypeName()
            is GeneratedFieldType -> ClassName(
                fieldType.packageName,
                resolveTypeClassName(fieldType.typeName, isMessage, mutable)
            )

            is GeneratedEnumFieldType -> ClassName(
                fieldType.packageName,
                resolveTypeClassName(fieldType.typeName, isMessage, mutable)
            )

            is ArrayFieldTypeWrapper -> listClassName.parameterizedBy(
                resolveTypeName(
                    fieldType.delegateType,
                    isMessage,
                    mutable,
                    nullable
                )
            )
        }

        return if (!fieldType.isPrimitive && typeName.isNullable != nullable) {
            typeName.copy(nullable = nullable)
        } else {
            typeName
        }
    }

    fun resolveTypeClassName(typeName: String, isMessage: Boolean, mutable: Boolean): String {
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
        ClassName(domain.pkgName, resolveTypeClassName(domain.domainName, true, true))

    fun DomainMessage<*>.eventClassName(domain: ResolvedDomain) =
        ClassName(domain.pkgName, resolveTypeClassName(domain.domainName, true, false))

    fun DomainMessage<*>.eventHandlerName(): String {
        return "on" + name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } + "Event"
    }

    fun KClass<*>.star() = asClassName().parameterizedBy(STAR)
}