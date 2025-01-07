package io.kofa.platform.codegen.writer.kotlin

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.kofa.platform.api.util.AbstractMessageBusService
import io.kofa.platform.api.util.EventContext
import io.kofa.platform.codegen.domain.ResolvedDomain
import io.kofa.platform.codegen.writer.kotlin.KotlinGeneratorUtils.eventClassName
import io.kofa.platform.codegen.writer.kotlin.KotlinGeneratorUtils.eventHandlerName
import io.kofa.platform.codegen.writer.kotlin.KotlinGeneratorUtils.messageConstantsClassName
import io.kofa.platform.codegen.writer.kotlin.KotlinGeneratorUtils.messageHandlerClassName
import io.kofa.platform.codegen.writer.kotlin.KotlinGeneratorUtils.star
import kotlin.reflect.KClass

class DomainMessageHandlerWriter {
    fun generatedMessageHandlerFileSpec(domain: ResolvedDomain): FileSpec {
        val sealedMessageTypeName = KotlinGeneratorUtils.getSealedDomainMessageClassName(domain)
        val handlerTypeName = domain.messageHandlerClassName()

        val fileSpecBuilder = FileSpec.builder(handlerTypeName)

        val handlerTypeSpecBuilder = TypeSpec.classBuilder(handlerTypeName)
            .addModifiers(KModifier.ABSTRACT)
            .superclass(
                AbstractMessageBusService::class.asTypeName()
                    .parameterizedBy(sealedMessageTypeName)
            )
            .addFunction(
                FunSpec.builder("isInterested")
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter(EVENT_TYPE_PARAMETER_NAME, KClass::class.star())
                    .returns(Boolean::class)
                    .addCode(buildInterestedCodeBlock(domain))
                    .build()
            )
            .addFunction(
                FunSpec.builder("dispatch")
                    .addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
                    .addTypeVariable(TypeVariableName("T"))
                    .addParameter("ctx", EventContext::class)
                    .addParameter(EVENT_PARAMETER_NAME, TypeVariableName("T"))
                    .addCode(buildDispatchCodeBlock(domain))
                    .build()
            )

        domain.messages.forEach { message ->
            handlerTypeSpecBuilder.addFunction(
                FunSpec.builder(message.eventHandlerName())
                    .addModifiers(KModifier.OPEN, KModifier.PROTECTED, KModifier.SUSPEND)
                    .addParameter(EVENT_PARAMETER_NAME, message.eventClassName(domain))
                    .addCode("")
                    .build()
            )
        }

        return fileSpecBuilder.addType(handlerTypeSpecBuilder.build()).build()
    }

    private fun buildInterestedCodeBlock(domain: ResolvedDomain): CodeBlock {
        val builder = CodeBlock.builder()

        builder.addStatement(
            "return %T.tryGetMessageMeta(%N) != null",
            domain.messageConstantsClassName(),
            EVENT_TYPE_PARAMETER_NAME
        )
        return builder.build()
    }

    private fun buildDispatchCodeBlock(domain: ResolvedDomain): CodeBlock {
        val builder = CodeBlock.builder()

        builder.beginControlFlow("when (%N)", EVENT_PARAMETER_NAME)
        domain.messages.forEach { message ->
            builder.addStatement(
                "is %T -> %N(%N)",
                message.eventClassName(domain),
                message.eventHandlerName(),
                EVENT_PARAMETER_NAME
            )
        }

        builder.endControlFlow()
        return builder.build()
    }

    companion object {
        const val EVENT_TYPE_PARAMETER_NAME = "eventType"
        const val EVENT_PARAMETER_NAME = "event"
    }
}