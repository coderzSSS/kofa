package io.kofa.platform.codegen.writer.kotlin

import com.squareup.kotlinpoet.*
import io.kofa.platform.api.codec.DirectBufferCodec
import io.kofa.platform.codegen.domain.ResolvedDomain
import io.kofa.platform.codegen.domain.type.DomainFieldType
import io.kofa.platform.codegen.domain.type.GeneratedFieldType
import io.kofa.platform.codegen.writer.kotlin.KotlinGeneratorUtils.messageClassName
import io.kofa.platform.codegen.writer.kotlin.KotlinGeneratorUtils.messageCodecClassName
import org.agrona.DirectBuffer
import org.agrona.MutableDirectBuffer

class DomainMessageCodecWriter {
    fun generate(domain: ResolvedDomain): FileSpec {
        val className = domain.messageCodecClassName()
        val typeSpecBuilder = TypeSpec.classBuilder(className)
            .addSuperinterface(DirectBufferCodec::class)
            .addProperty(
                encoderPropertyName(MESSAGE_HEADER_NAME),
                domain.encoderClassName(MESSAGE_HEADER_NAME),
                KModifier.PRIVATE
            )
            .addProperty(
                decoderPropertyName(MESSAGE_HEADER_NAME),
                domain.decoderClassName(MESSAGE_HEADER_NAME),
                KModifier.PRIVATE
            )

        domain.messages.forEach { message ->
            typeSpecBuilder.addProperty(
                encoderPropertyName(message.name),
                domain.encoderClassName(message.name),
                KModifier.PRIVATE
            )
            typeSpecBuilder.addProperty(
                decoderPropertyName(message.name),
                domain.decoderClassName(message.name),
                KModifier.PRIVATE
            )
        }

        val typeVariable = TypeVariableName("T")

        typeSpecBuilder.addFunction(
            FunSpec.builder("encodeToDirectBuffer")
                .addModifiers(KModifier.OVERRIDE)
                .addTypeVariable(TypeVariableName("T"))
                .addParameter("value", typeVariable)
                .addParameter("byteBuffer", MutableDirectBuffer::class)
                .addParameter("offset", Int::class)
                .addCode(buildEncodeCodeBlock(domain))
                .returns(Int::class)
                .build()
        ).addFunction(
            FunSpec.builder("decodeFromDirectBuffer")
                .addModifiers(KModifier.OVERRIDE)
                .addTypeVariable(TypeVariableName("T"))
                .addParameter("byteBuffer", DirectBuffer::class)
                .addParameter("offset", Int::class)
                .addCode(buildDecodeCodeBlock(domain))
                .returns(typeVariable)
                .build()
        )
        return FileSpec.builder(className)
            .addType(typeSpecBuilder.build())
            .build()
    }

    private fun buildEncodeCodeBlock(domain: ResolvedDomain): CodeBlock {
        val builder = CodeBlock.builder()
        builder.beginControlFlow("when(value)")
        domain.messages.forEach { message ->
            val encoderPropertyName = encoderPropertyName(message.name)
            builder.beginControlFlow("is %T -> ", message.messageClassName(domain))
                .addStatement(
                    "%N.wrapAndApplyHeader(byteBuffer, offset, %N)",
                    encoderPropertyName,
                    encoderPropertyName(MESSAGE_HEADER_NAME)
                )

            message.fields.forEach { field ->
                builder.add(buildEncodeFieldCodeBlock(encoderPropertyName, "value", field.name, field.type))
            }

            builder.endControlFlow()
        }

        builder.addStatement("else -> throw %T(\"unknown value type \$value\")", IllegalArgumentException::class)
        builder.endControlFlow()
        return builder.build()
    }

    private fun buildEncodeFieldCodeBlock(
        typeEncoderName: String,
        valueName: String,
        fieldName: String,
        fieldType: DomainFieldType
    ): CodeBlock {
        val builder = CodeBlock.builder()
        builder.beginControlFlow("if (%N.%N != null)", valueName, fieldName)
        if (fieldType.isPrimitive || fieldType.isEnum || fieldType.isBoolean) {
            builder.addStatement("%1N.%2N(%3N.%2N)", typeEncoderName, fieldName, valueName)
        } else if (fieldType is GeneratedFieldType) {
            fieldType.fields.forEach { field ->
                val typeFieldName = if (fieldType.isMessage) {
                    flattenFieldName(fieldName, field.name)
                } else {
                    fieldName
                }

                val fieldTypeEncoderName =
                    if (!field.type.isPrimitive && !field.type.isEnum && !field.type.isBoolean) {
                        val name = "${typeFieldName}Encoder";
                        builder.addStatement("val ${typeFieldName}Encoder = $typeEncoderName.${typeFieldName}()")
                        name
                    } else {
                        "${typeEncoderName}.$typeFieldName"
                    }

                builder.add(
                    buildEncodeFieldCodeBlock(
                        fieldTypeEncoderName,
                        "$valueName.$fieldName",
                        field.name,
                        field.type
                    )
                )
            }

        } else {
            throw IllegalStateException("cannot handle field $fieldName, type $fieldType")
        }

        builder.endControlFlow()

        return builder.build()
    }

    private fun buildDecodeCodeBlock(domain: ResolvedDomain): CodeBlock {
        val builder = CodeBlock.builder()

        return builder.build()
    }

    private fun ResolvedDomain.encoderClassName(name: String): ClassName {
        return ClassName(pkgName, name + "Encoder")
    }

    private fun ResolvedDomain.decoderClassName(name: String): ClassName {
        return ClassName(pkgName, name + "Decoder")
    }

    private fun encoderPropertyName(name: String) =
        name.replaceFirstChar { if (it.isUpperCase()) it.lowercase() else it.toString() } + "Encoder"

    private fun decoderPropertyName(name: String) =
        name.replaceFirstChar { if (it.isUpperCase()) it.lowercase() else it.toString() } + "Decoder"

    private fun flattenFieldName(vararg name: String): String {
        return buildString {
            val iterator = name.iterator()
            append(iterator.next())
            while (iterator.hasNext()) {
                append(iterator.next().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() })
            }
        }
    }

    companion object {
        const val MESSAGE_HEADER_NAME = "MessageHeader"
    }
}