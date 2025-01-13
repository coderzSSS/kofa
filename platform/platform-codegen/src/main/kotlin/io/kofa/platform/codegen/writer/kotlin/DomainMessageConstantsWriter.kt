package io.kofa.platform.codegen.writer.kotlin

import com.squareup.kotlinpoet.*
import io.kofa.platform.api.meta.MessageMeta
import io.kofa.platform.api.meta.MessageMetaProvider
import io.kofa.platform.codegen.domain.ResolvedDomain
import io.kofa.platform.codegen.writer.kotlin.KotlinGeneratorUtils.eventClassName
import io.kofa.platform.codegen.writer.kotlin.KotlinGeneratorUtils.messageConstantsClassName
import io.kofa.platform.codegen.writer.kotlin.KotlinGeneratorUtils.metaPropertyName
import io.kofa.platform.codegen.writer.kotlin.KotlinGeneratorUtils.star
import kotlin.reflect.KClass

class DomainMessageConstantsWriter {
    fun generateMessageConstants(domain: ResolvedDomain): FileSpec {
        val messageConstantsClassName = domain.messageConstantsClassName()
        val fileSpecBuilder = FileSpec.builder(messageConstantsClassName)

        val typeSpecBuilder = TypeSpec.objectBuilder(messageConstantsClassName)
            .addSuperinterface(MessageMetaProvider::class)
            .addFunction(
                FunSpec.builder("tryGetDomainClass")
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter(PARAM_NAME_EVENT_TYPE, Int::class)
                    .returns(KClass::class.star().copy(nullable = true))
                    .addCode(buildGetDomainClassCodeBlock(domain))
                    .build()
            )
            .addFunction(
                FunSpec.builder("tryGetMessageMeta")
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter(PARAM_NAME_EVENT_TYPE, KClass::class.star())
                    .returns(MessageMeta::class.asTypeName().copy(nullable = true))
                    .addCode(buildGetMessageMetaByClassCodeBlock(domain))
                    .build()
            )
            .addFunction(
                FunSpec.builder("tryGetMessageMeta")
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter(PARAM_NAME_EVENT_TYPE, Int::class)
                    .returns(MessageMeta::class.asTypeName().copy(nullable = true))
                    .addCode(buildGetMessageMetaByTypeCodeBlock(domain))
                    .build()
            )


        domain.messages.forEach { message ->
            typeSpecBuilder.addProperty(
                PropertySpec.builder(message.metaPropertyName(), MessageMeta::class)
                    .initializer("%T(%L, %L, %S)", MessageMeta::class, message.id, 0, domain.domainName)
                    .build()
            )
        }

        return fileSpecBuilder.addType(typeSpecBuilder.build()).build()
    }

    private fun buildGetDomainClassCodeBlock(domain: ResolvedDomain): CodeBlock {
        val builder = CodeBlock.builder()
        builder.beginControlFlow("return when (%N)", PARAM_NAME_EVENT_TYPE)

        domain.messages.forEach { message ->
            builder.addStatement("%L -> %T::class", message.id, message.eventClassName(domain))
        }

        builder.addStatement("else -> null")
        builder.endControlFlow()

        return builder.build()
    }

    private fun buildGetMessageMetaByClassCodeBlock(domain: ResolvedDomain): CodeBlock {
        val builder = CodeBlock.builder()
        builder.beginControlFlow("return when (%N)", PARAM_NAME_EVENT_TYPE)

        domain.messages.forEach { message ->
            builder.addStatement("%T::class -> %N", message.eventClassName(domain), message.metaPropertyName())
        }

        builder.addStatement("else -> null")
        builder.endControlFlow()

        return builder.build()
    }

    private fun buildGetMessageMetaByTypeCodeBlock(domain: ResolvedDomain): CodeBlock {
        val builder = CodeBlock.builder()
        builder.beginControlFlow("return when (%N)", PARAM_NAME_EVENT_TYPE)

        domain.messages.forEach { message ->
            builder.addStatement("%L -> %N", message.id, message.metaPropertyName())
        }

        builder.addStatement("else -> null")
        builder.endControlFlow()

        return builder.build()
    }

    companion object {
        const val PARAM_NAME_EVENT_TYPE = "eventType"
    }
}