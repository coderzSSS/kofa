package io.kofa.platform.codegen.writer.kotlin

import com.squareup.kotlinpoet.*
import io.kofa.platform.api.codec.DirectBufferCodec
import io.kofa.platform.codegen.domain.ResolvedDomain
import io.kofa.platform.codegen.domain.type.DomainFieldType
import io.kofa.platform.codegen.domain.type.GeneratedFieldType
import io.kofa.platform.codegen.domain.type.JavaBuiltinType
import io.kofa.platform.codegen.writer.kotlin.KotlinGeneratorUtils.cap
import io.kofa.platform.codegen.writer.kotlin.KotlinGeneratorUtils.deCap
import io.kofa.platform.codegen.writer.kotlin.KotlinGeneratorUtils.flattenFieldName
import io.kofa.platform.codegen.writer.kotlin.KotlinGeneratorUtils.generatedClassName
import io.kofa.platform.codegen.writer.kotlin.KotlinGeneratorUtils.isNeedFlatten
import io.kofa.platform.codegen.writer.kotlin.KotlinGeneratorUtils.messageClassName
import io.kofa.platform.codegen.writer.kotlin.KotlinGeneratorUtils.messageCodecClassName
import io.kofa.platform.codegen.writer.kotlin.KotlinGeneratorUtils.sbeBooleanType
import io.kofa.platform.codegen.writer.kotlin.KotlinGeneratorUtils.sbeClassName
import io.kofa.platform.codegen.writer.kotlin.KotlinGeneratorUtils.simpleClassName
import org.agrona.DirectBuffer
import org.agrona.MutableDirectBuffer

class DomainMessageCodecWriter {
    fun generate(domain: ResolvedDomain): FileSpec {
        val className = domain.messageCodecClassName()
        val typeSpecBuilder = TypeSpec.classBuilder(className)
            .addSuperinterface(DirectBufferCodec::class)
            .addProperty(buildCodecProperty(domain, MESSAGE_HEADER_NAME, true))
            .addProperty(buildCodecProperty(domain, MESSAGE_HEADER_NAME, false))

        domain.messages.forEach { message ->
            typeSpecBuilder
                .addProperty(buildCodecProperty(domain, message.name, true))
                .addProperty(buildCodecProperty(domain, message.name, false))
        }

        val typeVariable = TypeVariableName("T")

        typeSpecBuilder.addFunction(
            FunSpec.builder("encodeToDirectBuffer")
                .addModifiers(KModifier.OVERRIDE)
                .addTypeVariable(TypeVariableName("T"))
                .addParameter(VALUE_NAME, typeVariable)
                .addParameter("byteBuffer", MutableDirectBuffer::class)
                .addParameter("offset", Int::class)
                .addCode(buildEncodeCodeBlock(domain))
                .returns(Int::class)
                .build()
        ).addFunction(
            FunSpec.builder("decodeFromDirectBuffer")
                .addModifiers(KModifier.OVERRIDE)
                .addAnnotation(
                    AnnotationSpec.builder(Suppress::class)
                        .addMember("%S", "UNCHECKED_CAST")
                        .build()
                )
                .addTypeVariable(TypeVariableName("T"))
                .addParameter("byteBuffer", DirectBuffer::class)
                .addParameter("offset", Int::class)
                .addCode(buildDecodeCodeBlock(domain))
                .returns(typeVariable)
                .build()
        )
        if (domain.messages.any { type -> type.fields.any { f -> f.type.isBoolean } } || domain.types.any { type -> type.fields.any { f -> f.type.isBoolean } }) {
            typeSpecBuilder.addFunction(
                FunSpec.builder(TO_SBE_BOOL_FUN)
                    .addModifiers(KModifier.PRIVATE)
                    .addParameter("value", Boolean::class)
                    .addCode("return if(value) %1T.T else %1T.F", domain.sbeBooleanType())
                    .returns(domain.sbeBooleanType())
                    .build()
            ).addFunction(
                FunSpec.builder(FROM_SBE_BOOL_FUN)
                    .addModifiers(KModifier.PRIVATE)
                    .addParameter("value", domain.sbeBooleanType())
                    .addCode("return if(value == %1T.T) true else false", domain.sbeBooleanType())
                    .returns(Boolean::class)
                    .build()
            )
        }

        domain.enums.forEach { type ->
            typeSpecBuilder.addFunction(
                FunSpec.builder(TO_SBE_ENUM_FUN)
                    .addModifiers(KModifier.PRIVATE)
                    .addParameter("value", domain.simpleClassName(type.name))
                    .addCode(
                        "return %1T.entries.single { e -> e.value().toInt() == value.${DomainMessageWriter.CODE_NAME} }",
                        domain.sbeClassName(type.name)
                    )
                    .returns(domain.sbeClassName(type.name))
                    .build()
            ).addFunction(
                FunSpec.builder(FROM_SBE_ENUM_FUN)
                    .addModifiers(KModifier.PRIVATE)
                    .addParameter("value", domain.sbeClassName(type.name))
                    .addCode(
                        "return %1T.entries.single { e -> value.value().toInt() == e.${DomainMessageWriter.CODE_NAME} }",
                        domain.simpleClassName(type.name)
                    )
                    .returns(domain.simpleClassName(type.name))
                    .build()
            )
        }

        return FileSpec.builder(className)
            .addType(typeSpecBuilder.build())
            .build()
    }

    private fun buildCodecProperty(domain: ResolvedDomain, name: String, isEncoder: Boolean): PropertySpec {
        val typeName = if (isEncoder) {
            domain.encoderClassName(name)
        } else {
            domain.decoderClassName(name)
        }

        val propertyName = if (isEncoder) {
            encoderPropertyName(name)
        } else {
            decoderPropertyName(name)
        }
        return PropertySpec.builder(propertyName, typeName)
            .addModifiers(KModifier.PRIVATE)
            .initializer("%T()", typeName)
            .build()
    }

    private fun buildEncodeCodeBlock(domain: ResolvedDomain): CodeBlock {
        val builder = CodeBlock.builder()
        builder.beginControlFlow("when(%N)", VALUE_NAME)
        domain.messages.forEach { message ->
            val encoderPropertyName = encoderPropertyName(message.name)
            val messageHeaderPropertyName = encoderPropertyName(MESSAGE_HEADER_NAME)
            builder.beginControlFlow("is %T -> ", message.messageClassName(domain))
                .addStatement(
                    "%N.wrapAndApplyHeader(byteBuffer, offset, %N)",
                    encoderPropertyName,
                    messageHeaderPropertyName
                )

            message.fields.forEach { field ->
                val localEncoderVars = mutableSetOf<String>()
                builder.add(
                    buildEncodeFieldCodeBlock(
                        encoderPropertyName,
                        VALUE_NAME,
                        field.name,
                        false,
                        field.name,
                        field.type,
                        domain,
                        localEncoderVars
                    )
                )
            }

            builder.addStatement(
                "return %T.ENCODED_LENGTH + %N.encodedLength()",
                domain.encoderClassName(MESSAGE_HEADER_NAME),
                encoderPropertyName
            )
            builder.endControlFlow()
        }

        builder.addStatement("else -> throw %T(\"unknown value type \$$VALUE_NAME}\")", IllegalArgumentException::class)
        builder.endControlFlow()
        return builder.build()
    }

    private fun buildEncodeFieldCodeBlock(
        typeEncoderName: String,
        valueName: String,
        valueFieldName: String,
        isFieldValueNullable: Boolean,
        encoderFieldName: String,
        fieldType: DomainFieldType,
        domain: ResolvedDomain,
        localEncoderVars: MutableSet<String>
    ): CodeBlock {
        val builder = CodeBlock.builder()
        if (isFieldValueNullable) {
            builder.beginControlFlow("%L?.%L?.let", valueName, valueFieldName)
        }

        if (fieldType.isPrimitive || fieldType.isEnum || fieldType.isBoolean || fieldType == JavaBuiltinType.STRING) {
            var valueStatement = if (isFieldValueNullable) {
                "it"
            } else {
                "%3L.%4L"
            }

            if (fieldType.isBoolean) {
                valueStatement = "${TO_SBE_BOOL_FUN}($valueStatement)"
            } else if (fieldType.isGenerated && fieldType.isEnum) {
                valueStatement = "${TO_SBE_ENUM_FUN}($valueStatement)"
            }

            if (isFieldValueNullable) {
                builder.addStatement("%L.%L($valueStatement)", typeEncoderName, encoderFieldName)
            } else {
                builder.addStatement(
                    "%1L.%2L($valueStatement)",
                    typeEncoderName,
                    encoderFieldName,
                    valueName,
                    valueFieldName
                )
            }
        } else if (fieldType is GeneratedFieldType) {
            val needFlatten = isNeedFlatten(fieldType)
            val fieldTypeName = KotlinGeneratorUtils.resolveTypeName(domain, fieldType, true)

            fieldType.fields.forEach { entry ->
                val fieldTypeEncoderName =
                    if (needFlatten) {
                        val name = "${valueFieldName.deCap()}${fieldType.typeName.cap()}Encoder";
                        if (!localEncoderVars.contains(name)) {
                            builder.addStatement("var $name = $typeEncoderName.${valueFieldName}Count(1)")
                            localEncoderVars.add(name)
                        } else {
                            builder.addStatement("$name = $typeEncoderName.${valueFieldName}Count(1)")
                        }
                        name
                    } else {
                        "${typeEncoderName}.${valueFieldName}"
                    }

                val typeFieldName = if (needFlatten) {
                    flattenFieldName(valueFieldName, fieldType.typeName, entry.key)
                } else {
                    entry.key
                }

                builder.add(
                    buildEncodeFieldCodeBlock(
                        fieldTypeEncoderName,
                        "$valueName.$valueFieldName",
                        entry.key,
                        fieldTypeName.isNullable,
                        typeFieldName,
                        entry.value,
                        domain,
                        localEncoderVars,
                    )
                )
            }

        } else {
            throw IllegalStateException("cannot handle field $valueFieldName, type $fieldType")
        }

        if (isFieldValueNullable) {
            builder.endControlFlow()
        }

        return builder.build()
    }

    private fun buildDecodeCodeBlock(domain: ResolvedDomain): CodeBlock {
        val messageHeaderDecoderName = decoderPropertyName(MESSAGE_HEADER_NAME)

        val builder = CodeBlock.builder()
        builder.addStatement("%N.wrap(byteBuffer, offset)", messageHeaderDecoderName)
            .addStatement("val templateId = %N.templateId()", messageHeaderDecoderName)
            .addStatement("val actingBlockLength = %N.blockLength()", messageHeaderDecoderName)
            .addStatement("val actingVersion = %N.version()", messageHeaderDecoderName)
            .addStatement("val bufferOffset = %N.encodedLength() + offset", messageHeaderDecoderName)

        builder.beginControlFlow("when (templateId)")
        domain.messages.forEach { message ->
            builder.beginControlFlow("%T.TEMPLATE_ID -> ", domain.decoderClassName(message.name))
            val decoderName = decoderPropertyName(message.name)
            builder.addStatement("%N.wrap(byteBuffer, bufferOffset, actingBlockLength, actingVersion)", decoderName)

            message.fields.forEach { field ->
                val valueVar = field.name.decodedValueVar()
                val localDecoderVars = mutableSetOf<String>()
                builder.add(
                    buildDecodeFieldCodeBlock(
                        decoderName,
                        valueVar,
                        field.name,
                        field.name,
                        field.type,
                        localDecoderVars,
                        domain
                    )
                )
            }

            builder.add("return %T(\n", domain.messageClassName(message.name))
                .indent()

            message.fields.forEach { field ->
                builder.addStatement("%N = %N,", field.name, field.name.decodedValueVar())
            }

            builder.unindent().add(") as T\n")

            builder.endControlFlow()
        }

        builder.addStatement("else -> throw %T(%S)", IllegalStateException::class, "unknown template id \${templateId}")

        // end when
        builder.endControlFlow()
        return builder.build()
    }

    fun String.decodedValueVar() = "${this}Decoded"

    private fun buildDecodeFieldCodeBlock(
        typeDecoderName: String,
        valueVarName: String,
        valueFieldName: String,
        decoderFieldName: String,
        fieldType: DomainFieldType,
        localDecoderVars: MutableSet<String>,
        domain: ResolvedDomain
    ): CodeBlock {
        val builder = CodeBlock.builder()

        if (fieldType.isPrimitive || fieldType.isEnum || fieldType.isBoolean || fieldType == JavaBuiltinType.STRING) {
            var valueStatement = "%L.%L()"

            if (fieldType.isBoolean) {
                valueStatement = "${FROM_SBE_BOOL_FUN}($valueStatement)"
            } else if (fieldType.isGenerated && fieldType.isEnum) {
                valueStatement = "${FROM_SBE_ENUM_FUN}($valueStatement)"
            }

            builder.addStatement(
                "val %N = $valueStatement",
                valueVarName,
                typeDecoderName,
                decoderFieldName,
            )
        } else if (fieldType is GeneratedFieldType) {
            val needFlatten = isNeedFlatten(fieldType)

            fieldType.fields.forEach { entry ->
                val fieldTypeDecoderName =
                    if (needFlatten) {
                        val name = "${valueFieldName.deCap()}${fieldType.typeName.cap()}Decoder";
                        if (!localDecoderVars.contains(name)) {
                            builder.addStatement("val $name = $typeDecoderName.${valueFieldName}()")
                            localDecoderVars.add(name)
                        }
                        name
                    } else {
                        "${typeDecoderName}.${valueFieldName}"
                    }

                val typeFieldName = if (needFlatten) {
                    flattenFieldName(valueFieldName, fieldType.typeName, entry.key)
                } else {
                    entry.key
                }

                builder.add(
                    buildDecodeFieldCodeBlock(
                        fieldTypeDecoderName,
                        typeFieldName.decodedValueVar(),
                        entry.key,
                        typeFieldName,
                        entry.value,
                        localDecoderVars,
                        domain
                    )
                )
            }

            builder.add("val %N = %T(\n", valueVarName, domain.generatedClassName(fieldType))
                .indent()

            fieldType.fields.forEach { entry ->
                val typeFieldName = if (needFlatten) {
                    flattenFieldName(valueFieldName, fieldType.typeName, entry.key)
                } else {
                    entry.key
                }

                builder.addStatement("%N = %N,", entry.key, typeFieldName.decodedValueVar())
            }

            builder.unindent().add(")\n")

        } else {
            throw IllegalStateException("cannot handle field $valueFieldName, type $fieldType")
        }

        return builder.build()
    }

    private fun ResolvedDomain.encoderClassName(name: String): ClassName {
        return ClassName("$pkgName.sbe", name.cap() + "Encoder")
    }

    private fun ResolvedDomain.decoderClassName(name: String): ClassName {
        return ClassName("$pkgName.sbe", name.cap() + "Decoder")
    }

    private fun encoderPropertyName(name: String) =
        name.deCap() + "Encoder"

    private fun decoderPropertyName(name: String) =
        name.deCap() + "Decoder"

    companion object {
        const val MESSAGE_HEADER_NAME = "MessageHeader"
        const val VALUE_NAME = "obj"
        const val TO_SBE_BOOL_FUN = "toSbeBooleanType"
        const val FROM_SBE_BOOL_FUN = "fromSbeBooleanType"
        const val TO_SBE_ENUM_FUN = "toSbeEnumType"
        const val FROM_SBE_ENUM_FUN = "fromSbeEnumType"
    }
}