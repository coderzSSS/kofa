package io.kofa.platform.codegen.writer.kotlin

import com.squareup.kotlinpoet.*
import io.kofa.platform.codegen.domain.DomainEnumField
import io.kofa.platform.codegen.domain.DomainType
import io.kofa.platform.codegen.domain.ResolvedDomain
import io.kofa.platform.codegen.domain.ResolvedDomainField

class DomainMessageWriter {
    fun generateDomainMessageFileSpec(domain: ResolvedDomain): FileSpec {
        checkNotNull(domain.pkgName) { "domain package name must be provided" }
        checkNotNull(domain.domainName) { "domain name must be provided" }
        check(domain.messages.isNotEmpty()) { "domain messages must be defined" }

        val sealedMessageType =
            TypeSpec.interfaceBuilder(KotlinGeneratorUtils.getSealedDomainMessageName(domain.domainName))
                .addModifiers(KModifier.SEALED).build()
        val sealedMessageTypeName = KotlinGeneratorUtils.getSealedDomainMessageClassName(domain)

        val fileSpecBuilder = FileSpec.builder(domain.pkgName, domain.domainName + "Messages")
            .addFileComment("GENERATED FILE, DO NOT EDIT")
            .addType(sealedMessageType)
            .addTypes(domain.enums.map { type -> generateDomainEnum(type) })
            .addTypes(domain.types.map { type -> generateDomainEvent(domain, type.name, type.fields, false) })
            .addTypes(domain.types.map { type -> generateMutableDomainMessage(domain, type.name, type.fields, false) })
            .addTypes(domain.messages.map { type ->
                generateDomainEvent(
                    domain,
                    type.name,
                    type.fields,
                    true,
                    sealedMessageTypeName
                )
            })
            .addTypes(domain.messages.map { type ->
                generateMutableDomainMessage(
                    domain,
                    type.name,
                    type.fields,
                    true
                )
            })

        return fileSpecBuilder.build()
    }

    private fun generateDomainEvent(
        domain: ResolvedDomain,
        name: String,
        fields: List<ResolvedDomainField>,
        isMessage: Boolean,
        interfaceTypeName: TypeName? = null
    ): TypeSpec {
        val clazzName = ClassName(domain.pkgName, KotlinGeneratorUtils.resolveTypeClassName(name, isMessage, false))

        val typeSpecBuilder = TypeSpec.interfaceBuilder(clazzName)

        interfaceTypeName?.let {
            typeSpecBuilder.addSuperinterface(it)
        }
        fields.forEach { field ->
            val typeName = KotlinGeneratorUtils.resolveTypeName(field.type, isMessage, false, true)
            typeSpecBuilder.addProperty(field.name, typeName)
        }

        return typeSpecBuilder.addFunction(
            FunSpec.builder("duplicate")
                .returns(clazzName)
                .build()
        ).build()
    }

    private fun generateMutableDomainMessage(
        domain: ResolvedDomain,
        name: String,
        fields: List<ResolvedDomainField>,
        isMessage: Boolean
    ): TypeSpec {
        val eventInterfaceClassName =
            ClassName(domain.pkgName, KotlinGeneratorUtils.resolveTypeClassName(name, isMessage, false))

        val messageClassName = ClassName(domain.pkgName, KotlinGeneratorUtils.resolveTypeClassName(name, isMessage, true))

        val messageTypeSpecBuilder = TypeSpec.classBuilder(messageClassName)
                .addModifiers(KModifier.DATA)
                .addSuperinterface(eventInterfaceClassName)

        val primaryConstructorBuilder = FunSpec.constructorBuilder()

        fields.forEach { field ->
            val typeName = KotlinGeneratorUtils.resolveTypeName(field.type, isMessage, true, true)

            val parameterBuilder = ParameterSpec.builder(field.name, typeName).addModifiers(KModifier.OVERRIDE)
            if (typeName.isNullable) {
                parameterBuilder.defaultValue("null")
            }

            primaryConstructorBuilder.addParameter(parameterBuilder.build())

            messageTypeSpecBuilder.addProperty(
                PropertySpec.builder(field.name, typeName)
                    .addModifiers(KModifier.OVERRIDE)
                    .initializer(field.name)
                    .mutable(true)
                    .build()
            )
        }

        messageTypeSpecBuilder.addFunction(
            FunSpec.builder("duplicate")
                .addModifiers(KModifier.OVERRIDE)
                .returns(messageClassName)
                .addStatement("return copy()")
                .build()
        )
        return messageTypeSpecBuilder.build()
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