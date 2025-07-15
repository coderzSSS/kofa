package io.kofa.platform.codegen.writer.kotlin

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.kofa.platform.api.codec.CodecUtils
import io.kofa.platform.api.codec.DirectBufferCodec
import io.kofa.platform.api.message.EventHeader
import io.kofa.platform.codegen.domain.ResolvedDomain
import io.kofa.platform.codegen.domain.type.ArrayFieldTypeWrapper
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
                .addParameter("header", EventHeader::class)
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
                .returns(Pair::class.asClassName().parameterizedBy(listOf(EventHeader::class.asTypeName(), typeVariable)))
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
            builder.addStatement("%N.eventTimestamp(header.eventTimeStampInMillis)", messageHeaderPropertyName)
                .addStatement("%N.sourceSequence(header.sourceSequence)", messageHeaderPropertyName)
                .addStatement("%N.globalSequence(header.globalSequence)", messageHeaderPropertyName)
                .addStatement("%N.source(%T.encodeSourceToInt(header.source))", messageHeaderPropertyName, CodecUtils::class)

            val localEncoderVars = mutableSetOf<String>()
            message.fields.forEach { field ->
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
        localEncoderVars: MutableSet<String>,
        insideArray: Boolean = false
    ): CodeBlock {
        val builder = CodeBlock.builder()
        if (isFieldValueNullable) {
            if (insideArray) {
                builder.beginControlFlow("%L.%L?.let", valueName, valueFieldName)
            } else {
                builder.beginControlFlow("%L?.%L?.let", valueName, valueFieldName)
            }
        }

        if (fieldType.isPrimitive || fieldType.isEnum || fieldType.isBoolean || fieldType == JavaBuiltinType.STRING) {
            var valueStatement = if (isFieldValueNullable) {
                "it"
            } else if (insideArray) {
                "%3L"
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
            } else if (insideArray) {
                builder.addStatement(
                    "%1L.%2L($valueStatement)",
                    typeEncoderName,
                    encoderFieldName,
                    valueName
                )
            } else {
                builder.addStatement(
                    "%1L.%2L($valueStatement)",
                    typeEncoderName,
                    encoderFieldName,
                    valueName,
                    valueFieldName
                )
            }
        } else if (fieldType is ArrayFieldTypeWrapper || fieldType is GeneratedFieldType) {
            val needFlatten = isNeedFlatten(fieldType)
            val fieldTypeName = KotlinGeneratorUtils.resolveTypeName(domain, fieldType, true)


            val fieldTypeEncoderName =
                if (needFlatten) {
                    "${valueFieldName.deCap()}${fieldType.typeName.cap()}Encoder";
                } else {
                    "${typeEncoderName}.${valueFieldName}"
                }

            if (needFlatten && !insideArray) {
                val countVar = if (localEncoderVars.contains("count")) {
                    "count"
                } else {
                    localEncoderVars.add("count")
                    "var count"
                }
                if (fieldType.isArray) {
                    builder.addStatement("%L = $valueName.${valueFieldName}.size", countVar)
                } else {
                    builder.addStatement("%L = 1", countVar)
                }

                builder.beginControlFlow("if (count > 0)")
                if (!localEncoderVars.contains(fieldTypeEncoderName)) {
                    builder.addStatement("var $fieldTypeEncoderName = $typeEncoderName.${valueFieldName}Count(count)")
                    localEncoderVars.add(fieldTypeEncoderName)
                } else {
                    builder.addStatement("$fieldTypeEncoderName = $typeEncoderName.${valueFieldName}Count(count)")
                }
            }

            if (fieldType is ArrayFieldTypeWrapper) {
                builder.beginControlFlow("$valueName.$valueFieldName.forEachIndexed { index, value ->")

                builder.add(
                    buildEncodeFieldCodeBlock(
                        fieldTypeEncoderName,
                        "value",
                        valueFieldName,
                        false,
                        flattenFieldName(valueFieldName, valueFieldName),
                        fieldType.delegateType,
                        domain,
                        localEncoderVars,
                        true
                    )
                )

                if (needFlatten) {
                    builder.beginControlFlow("if (index < (count -1))")
                    builder.addStatement("$fieldTypeEncoderName = $fieldTypeEncoderName.next()")
                    builder.endControlFlow()
                }

                builder.endControlFlow()
            } else if (fieldType is GeneratedFieldType) {
                fieldType.fields.forEach { entry ->
                    val typeFieldName = if (needFlatten) {
                        flattenFieldName(valueFieldName, fieldType.typeName, entry.key)
                    } else {
                        entry.key
                    }

                    val valueNameUpdated = if (!insideArray) {
                        "$valueName.$valueFieldName"
                    } else {
                        valueName
                    }

                    builder.add(
                        buildEncodeFieldCodeBlock(
                            fieldTypeEncoderName,
                            valueNameUpdated,
                            entry.key,
                            fieldTypeName.isNullable,
                            typeFieldName,
                            entry.value,
                            domain,
                            localEncoderVars,
                        )
                    )
                }
            }
            if (needFlatten && !insideArray) {
                builder.endControlFlow()
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

        builder.add("val header = %T(\n", EventHeader::class)
            .indent()
            .add("eventType = templateId,\n")
            .add("source = %T.decodeIntToSource(%N.source()),\n", CodecUtils::class, messageHeaderDecoderName)
            .add("sourceSequence = %N.sourceSequence(),\n", messageHeaderDecoderName)
            .add("globalSequence = %N.globalSequence(),\n", messageHeaderDecoderName)
            .add("eventTimeStampInMillis = %N.eventTimestamp()\n", messageHeaderDecoderName)
            .unindent()
            .add(")\n")

        builder.beginControlFlow("val value = when (templateId)")
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

            builder.add("%T(\n", domain.messageClassName(message.name))
                .indent()

            message.fields.forEach { field ->
                builder.addStatement("%N = %N,", field.name, field.name.decodedValueVar())
            }

            builder.unindent().add(") as T\n")

            builder.endControlFlow()
        }

        builder.addStatement("else -> throw %T(\"%L\")", IllegalStateException::class, "unknown template id \${templateId}")

        // end when
        builder.endControlFlow()
        builder.addStatement("return %T(header, value)", Pair::class)
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
        } else if (fieldType.isArray || fieldType is GeneratedFieldType) {
            val needFlatten = isNeedFlatten(fieldType)
            val fieldTypeDecoderName =
                if (needFlatten) {
                    val name = "${valueFieldName.deCap()}${fieldType.typeName.cap()}Decoder";
                    if (!localDecoderVars.contains(name)) {
                        builder.addStatement("var $name = $typeDecoderName.${valueFieldName}()")
                        localDecoderVars.add(name)
                    }
                    name
                } else {
                    "${typeDecoderName}.${valueFieldName}"
                }

            var valueVarNameUpdated = valueVarName
            if (needFlatten) {
                if (fieldType is ArrayFieldTypeWrapper) {
                    valueVarNameUpdated = "${valueVarName}Item"
                    builder.addStatement("val %N = mutableListOf<%T>()", valueVarName, domain.generatedClassName(fieldType.delegateType, false))
                    builder.beginControlFlow("while(%N.hasNext())", fieldTypeDecoderName)
                } else {
                    builder.addStatement("var %N: %T? = null ", valueVarName, domain.generatedClassName(fieldType))
                    builder.beginControlFlow("if(%N.hasNext())", fieldTypeDecoderName)
                }
                builder.addStatement("%1N = %1N.next()", fieldTypeDecoderName)
            }

            if (fieldType is GeneratedFieldType) {
                fieldType.fields.forEach { entry ->
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
            } else if (fieldType is ArrayFieldTypeWrapper) {
                builder.add(
                    buildDecodeFieldCodeBlock(
                        fieldTypeDecoderName,
                        valueVarNameUpdated,
                        valueFieldName,
                        flattenFieldName(valueFieldName, valueFieldName),
                        fieldType.delegateType,
                        localDecoderVars,
                        domain
                    )
                )
            }

            if (fieldType is GeneratedFieldType) {
                builder.add(" %N = %T(\n", valueVarNameUpdated, domain.generatedClassName(fieldType))
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
            }

            if (needFlatten) {
                if (fieldType.isArray) {
                    builder.addStatement("%N?.let { %N.add(it) }", valueVarNameUpdated, valueVarName)
                }
                builder.endControlFlow()
            }

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