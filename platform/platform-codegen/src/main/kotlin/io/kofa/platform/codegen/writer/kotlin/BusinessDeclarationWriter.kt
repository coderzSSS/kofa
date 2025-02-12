package io.kofa.platform.codegen.writer.kotlin

import com.google.auto.service.AutoService
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import io.kofa.platform.api.dsl.BusinessDeclaration
import io.kofa.platform.api.util.EventContext
import io.kofa.platform.codegen.domain.ResolvedDomain
import io.kofa.platform.codegen.writer.kotlin.KotlinGeneratorUtils.businessDeclarationClassName
import io.kofa.platform.codegen.writer.kotlin.KotlinGeneratorUtils.eventClassName
import io.kofa.platform.codegen.writer.kotlin.KotlinGeneratorUtils.messageClassName
import io.kofa.platform.codegen.writer.kotlin.KotlinGeneratorUtils.messageHandlerClassName
import io.kofa.platform.codegen.writer.kotlin.KotlinGeneratorUtils.sealedDomainMessageClassName
import io.kofa.platform.codegen.writer.kotlin.KspUtils.asKsType

class BusinessDeclarationWriter(private val resolver: Resolver) {
    data class ComponentTypeConfig(
        val componentType: String,
        val handlerClass: KSClassDeclaration,
        val moduleClass: KSClassDeclaration
    )

    fun generate(domain: ResolvedDomain, componentList: List<ComponentTypeConfig>): FileSpec {
        val clazzName = domain.businessDeclarationClassName()
        return FileSpec.builder(clazzName)
            .addType(
                TypeSpec.classBuilder(clazzName)
                    .superclass(
                        BusinessDeclaration::class.asTypeName().parameterizedBy(domain.sealedDomainMessageClassName())
                    )
                    .addSuperclassConstructorParameter(buildDslCodeBlock(domain, componentList))
                    .addAnnotation(
                        AnnotationSpec.builder(AutoService::class)
                            .addMember("%T::class", BusinessDeclaration::class)
                            .build()
                    )
                    .addAnnotation(
                        AnnotationSpec.builder(SuppressWarnings::class)
                            .addMember("%S", "rawtypes")
                            .build()
                    )
                    .build()
            )
            .build()
    }

    private fun buildDslCodeBlock(domain: ResolvedDomain, componentList: List<ComponentTypeConfig>): CodeBlock {
        val builder = CodeBlock.builder()
        builder.addStatement("{").indent()

        val componentMap = componentList.associateBy { c -> c.componentType }

        componentMap.forEach { entry ->
            val componentName = entry.key
            val config = entry.value

            builder.beginControlFlow("component (%S)", componentName)

            val functions = config.moduleClass.getAllFunctions()
                .filter { f ->
                    f.parameters.isEmpty() && f.returnType?.resolve()?.declaration?.qualifiedName?.asString() == "io.kofa.platform.api.inject.ComponentModuleDeclaration"
                }.toList()

            if (functions.isNotEmpty()) {
                val isObject = config.moduleClass.classKind == ClassKind.OBJECT || config.moduleClass.isCompanionObject
                val statement = if (isObject) {
                    "%T"
                } else {
                    check(
                        config.moduleClass.getConstructors()
                            .any { f -> f.parameters.isEmpty() }) { "no default constructor found for ${config.moduleClass.toClassName()}" }
                    "%T()"
                }

                builder.addStatement("val module = $statement", config.moduleClass.toClassName())

                functions.forEach { f ->
                    builder.addStatement("install(module.%N())", f.simpleName.asString())
                }
            }

            val isDomainMessageHandler = domain.messageHandlerClassName().asKsType(resolver)?.isAssignableFrom(config.handlerClass.asStarProjectedType()) == true

            val constructorParameterSize = config.handlerClass.primaryConstructor?.parameters?.size ?: 0
            val parameterStatement = if (constructorParameterSize > 0) {
                "get(), ".repeat(constructorParameterSize).removeSuffix(", ")
            } else {
                ""
            }

            val dslCodeBuilder = CodeBlock.builder().beginControlFlow("scoped")
            if (config.handlerClass.classKind == ClassKind.OBJECT) {
                dslCodeBuilder.addStatement("%T", config.handlerClass.toClassName())
            } else {
                dslCodeBuilder.addStatement("%T($parameterStatement)", config.handlerClass.toClassName())
            }

            dslCodeBuilder.endControlFlow()
            if (isDomainMessageHandler) {
                val bindMember = MemberName("org.koin.dsl", "bind")
                dslCodeBuilder.add(".%M(%T::class)", bindMember, domain.messageHandlerClassName())
            }

            builder.addStatement("install({")
            builder.indent().add(dslCodeBuilder.build())
            builder.unindent().addStatement("})")

            val injectMember = MemberName("io.kofa.platform.api.dsl", "inject")

            if (isDomainMessageHandler) {
                builder.addStatement("withEventDispatcher(%M<%T>())", injectMember, domain.messageHandlerClassName())
            } else {

                val domainMessages = domain.messages.flatMap { message ->
                    listOf(
                        domain.messageClassName(message.name),
                        domain.eventClassName(message.name)
                    )
                }

                val handlerFunctions = config.handlerClass.getAllFunctions().mapNotNull { f ->
                    val result = if (f.parameters.size == 1) {
                        val type = f.parameters.first().type.resolve().declaration.qualifiedName?.asString()
                        if (domainMessages.any { name -> name.canonicalName == type }) {
                            type!! to f.to(0)
                        } else {
                            null
                        }

                    } else if (f.parameters.size == 2) {
                        val type1 = f.parameters.first().type.resolve().declaration.qualifiedName?.asString()
                        val type2 = f.parameters.last().type.resolve().declaration.qualifiedName?.asString()
                        if (domainMessages.any { name -> name.canonicalName == type1 } && type2 == EventContext::class.qualifiedName) {
                            type1!! to f.to(0)
                        } else if (domainMessages.any { name -> name.canonicalName == type2 } && type1 == EventContext::class.qualifiedName) {
                            type2!! to f.to(1)
                        } else {
                            null
                        }
                    } else {
                        null
                    }

                    result
                }.toMap()

                if (handlerFunctions.isNotEmpty()) {
                    builder.addStatement("val handler: %T by %M()", config.handlerClass.toClassName(), injectMember)
                }

                handlerFunctions.forEach { e ->
                    val messageClassName = ClassName.bestGuess(e.key)
                    builder.addStatement("onEvent(%T::class) { event ->", messageClassName).indent()
                    val eventParameterIndex = e.value.second
                    val handlerFunction = e.value.first

                    if (eventParameterIndex == 0 && handlerFunction.parameters.size == 1) {
                        builder.addStatement("handler.%N(event)", handlerFunction.simpleName.asString())
                    } else if (eventParameterIndex == 0 && handlerFunction.parameters.size == 2) {
                        builder.addStatement("handler.%N(event, this)", handlerFunction.simpleName.asString())
                    } else if (eventParameterIndex == 1) {
                        builder.addStatement("handler.%N(this, event)", handlerFunction.simpleName.asString())
                    } else {
                        throw IllegalStateException("invalid handler function: $handlerFunction")
                    }

                    builder.unindent().addStatement("}")
                }
            }

            // end component dsl
            builder.endControlFlow()
        }

        builder.unindent().add("}")
        return builder.build()
    }
}