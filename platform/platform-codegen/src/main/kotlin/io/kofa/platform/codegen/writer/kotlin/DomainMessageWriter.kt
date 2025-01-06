package io.kofa.platform.codegen.writer.kotlin

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.ksp.writeTo
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.kofa.platform.codegen.domain.DomainEnumField
import io.kofa.platform.codegen.domain.DomainType
import io.kofa.platform.codegen.domain.ResolvedDomain
import io.kofa.platform.codegen.domain.ResolvedDomainField
import io.kofa.platform.codegen.domain.type.*

class DomainMessageWriter(private val logger: KSPLogger, private val codeGenerator: CodeGenerator) {
    fun generateDomainMessage(domain: ResolvedDomain) {
        checkNotNull(domain.pkgName) { "domain package name must be provided" }
        checkNotNull(domain.domainName) { "domain name must be provided" }
        check(domain.messages.isNotEmpty()) { "domain messages must be defined" }

        val fileSpecBuilder = FileSpec.builder(domain.pkgName, domain.domainName)
            .addFileComment("GENERATED FILE, DO NOT EDIT")
            .addTypes(domain.enums.map { type -> generateDomainEnum(type) })
            .addTypes(domain.types.map { type -> generateDomainMessage(type.name, type.fields, false, true) })
            .addTypes(domain.types.map { type -> generateDomainMessage(type.name, type.fields, false, false) })
            .addTypes(domain.messages.map { type -> generateDomainMessage(type.name, type.fields, true, true) })
            .addTypes(domain.messages.map { type -> generateDomainMessage(type.name, type.fields, true, false) })

        val fileSpec = fileSpecBuilder.build()
        logger.info("generating domain messages to ${fileSpec.relativePath}")

        fileSpec.writeTo(codeGenerator, Dependencies.ALL_FILES)
    }

    private fun generateDomainMessage(
        name: String,
        fields: List<ResolvedDomainField>,
        isMessage: Boolean,
        mutable: Boolean
    ): TypeSpec {
        val messageTypeSpecBuilder = TypeSpec.classBuilder(resolveTypeClassName(name, isMessage, mutable))
            .addModifiers(KModifier.DATA)

        val primaryConstructorBuilder = FunSpec.constructorBuilder()

        fields.forEach { field ->
            val typeName = resolveTypeName(field.type, isMessage, mutable, true)

            val parameterBuilder = ParameterSpec.builder(field.name, typeName)
            if (typeName.isNullable) {
                parameterBuilder.defaultValue("null")
            }

            primaryConstructorBuilder.addParameter(parameterBuilder.build())

            messageTypeSpecBuilder.addProperty(
                PropertySpec.builder(field.name, typeName)
                    .initializer(field.name)
                    .mutable(mutable)
                    .build()
            )
        }

        return messageTypeSpecBuilder.build()
    }

    private fun resolveTypeName(fieldType: DomainFieldType, isMessage: Boolean, mutable: Boolean, nullable: Boolean): TypeName {
        val listClassName = if (mutable) {
            MUTABLE_LIST
        } else {
            LIST
        }

        val typeName = when (fieldType) {
            is JavaBuiltinType -> fieldType.javaClass.asTypeName()
            is GeneratedFieldType -> ClassName(fieldType.packageName, resolveTypeClassName(fieldType.typeName, isMessage, mutable))
            is GeneratedEnumFieldType -> ClassName(fieldType.packageName, resolveTypeClassName(fieldType.typeName, isMessage, mutable))
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

    private fun resolveTypeClassName(typeName: String, isMessage: Boolean, mutable: Boolean): String {
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

    private fun generateDomainEnum(definition: DomainType<DomainEnumField>): TypeSpec {
        check(definition.fields.isNotEmpty()) { "missing fields for enum ${definition.name}" }

        val enumTypeSpecBuilder = TypeSpec.enumBuilder(definition.name)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("value", Int::class)
                    .addParameter(
                        ParameterSpec.builder("desc", String::class.asTypeName().copy(nullable = true))
                            .defaultValue("null")
                            .build()
                    )
                    .build()
            )
            .addProperty(
                PropertySpec.builder("desc", String::class).initializer("desc").build()
            )
            .addProperty(
                PropertySpec.builder("value", Int::class).initializer("value").build()
            )

        definition.fields.forEach { field ->
            checkNotNull(field.value) { "missing value for enum ${definition.name} field: ${field.name}" }
            enumTypeSpecBuilder.addEnumConstant(
                field.name,
                TypeSpec.anonymousClassBuilder()
                    .addSuperclassConstructorParameter("%N", field.value)
                    .build()
            )
        }

        return enumTypeSpecBuilder.build()
    }
}