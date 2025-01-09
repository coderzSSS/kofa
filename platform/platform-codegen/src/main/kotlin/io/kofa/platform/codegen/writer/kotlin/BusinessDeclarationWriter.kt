package io.kofa.platform.codegen.writer.kotlin

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import io.kofa.platform.api.dsl.BusinessDeclaration
import io.kofa.platform.api.inject.ComponentModuleDeclaration
import io.kofa.platform.codegen.domain.ResolvedDomain
import io.kofa.platform.codegen.writer.kotlin.KotlinGeneratorUtils.businessDeclarationClassName
import io.kofa.platform.codegen.writer.kotlin.KotlinGeneratorUtils.messageHandlerClassName
import io.kofa.platform.codegen.writer.kotlin.KotlinGeneratorUtils.sealedDomainMessageClassName
import io.kofa.platform.codegen.writer.kotlin.KspUtils.asKsType
import io.kofa.platform.codegen.writer.kotlin.KspUtils.isAssignableFrom

class BusinessDeclarationWriter(private val resolver: Resolver) {
    data class ComponentConfig(
        val componentType: String,
        val handlerClass: KSClassDeclaration,
        val moduleClass: KSClassDeclaration
    )

    fun generate(domain: ResolvedDomain, componentList: List<ComponentConfig>): FileSpec {
        val clazzName = domain.businessDeclarationClassName()
        return FileSpec.builder(clazzName)
            .addType(
                TypeSpec.classBuilder(clazzName)
                    .superclass(
                        BusinessDeclaration::class.asTypeName().parameterizedBy(domain.sealedDomainMessageClassName())
                    )
                    .addSuperclassConstructorParameter(buildDslCodeBlock(domain, componentList))
                    .build()
            )
            .build()
    }

    private fun buildDslCodeBlock(domain: ResolvedDomain, componentList: List<ComponentConfig>): CodeBlock {
        val builder = CodeBlock.builder()

        val componentMap = componentList.associateBy { c -> c.componentType }

        componentMap.forEach { entry ->
            val componentName = entry.key
            val config = entry.value

            builder.beginControlFlow("component (%S)", componentName)

            val functions = config.moduleClass.getAllFunctions()
                .filter { f ->
                    f.parameters.isEmpty() && f.returnType?.resolve()
                        ?.equals(ComponentModuleDeclaration::class.asKsType(resolver)) == true
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
                    builder.addStatement("install(module.%N())", f.simpleName)
                }
            }

            if (config.handlerClass.getAllSuperTypes()
                    .any { type -> type.isAssignableFrom(domain.messageHandlerClassName(), resolver) }
            ) {
                val bindMember = MemberName("org.koin.dsl", "bind")
                val constructorParameterSize = config.handlerClass.primaryConstructor?.parameters?.size ?: 0
                val parameterStatement = if (constructorParameterSize > 0) {
                    "get(), ".repeat(constructorParameterSize).removeSuffix(", ")
                } else {
                    ""
                }

                builder.beginControlFlow("install")
                builder.add(
                    CodeBlock.builder()
                        .beginControlFlow("scoped")
                        .addStatement("%T($parameterStatement)", config.handlerClass.toClassName())
                        .endControlFlow()
                        .add("%M(%T::class)", bindMember, domain.messageHandlerClassName())
                        .build()
                )
                builder.endControlFlow()

                val injectMember = MemberName("io.kofa.platform.api.dsl", "inject")
                builder.addStatement("withEventDispatcher(%M<%N>())", injectMember, domain.messageHandlerClassName())
            } else {
                // TODO: add individual event handlers
            }

            // end component dsl
            builder.endControlFlow()
        }

        return builder.build()
    }
}