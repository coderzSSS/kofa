package io.kofa.platform.codegen.writer.kotlin

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import io.kofa.platform.api.codec.DirectBufferCodec
import io.kofa.platform.api.dsl.DomainDeclaration
import io.kofa.platform.api.meta.MessageMetaProvider
import io.kofa.platform.codegen.domain.ResolvedDomain
import io.kofa.platform.codegen.writer.kotlin.KotlinGeneratorUtils.domainDeclarationClassName
import io.kofa.platform.codegen.writer.kotlin.KotlinGeneratorUtils.messageCodecClassName
import io.kofa.platform.codegen.writer.kotlin.KotlinGeneratorUtils.messageConstantsClassName
import io.kofa.platform.codegen.writer.kotlin.KotlinGeneratorUtils.qualifiedName

class DomainDeclarationWriter {
    fun generate(domain: ResolvedDomain): FileSpec {
        val clazzName = domain.domainDeclarationClassName()
        val fileSpecBuilder = FileSpec.builder(clazzName)
            .addAliasedImport(moduleMember, "koinModule")
            .addType(
                TypeSpec.classBuilder(clazzName)
                    .superclass(DomainDeclaration::class)
                    .addSuperclassConstructorParameter(buildDslCodeBlock(domain))
                    .addAnnotation(
                        AnnotationSpec.builder(AutoService::class)
                            .addMember("%T::class", DomainDeclaration::class)
                            .build()
                    )
                    .addAnnotation(
                        AnnotationSpec.builder(SuppressWarnings::class)
                            .addMember("%S", "rawtypes")
                            .build()
                    )
                    .build()
            )

        return fileSpecBuilder.build()
    }

    private fun buildDslCodeBlock(domain: ResolvedDomain): CodeBlock {
        val builder = CodeBlock.builder()

        // start dsl
        builder.addStatement("{").indent()
        builder.addStatement("domain = %S", domain.domainName)
        builder.addStatement("pkg = %S", domain.pkgName)

        val bindMember = MemberName("org.koin.dsl", "bind")
        val namedMember = MemberName("org.koin.core.qualifier", "named")
        val qualifier = domain.qualifiedName()

        builder.beginControlFlow("val domainModule = %M", moduleMember)
        builder.beginControlFlow("factory(%M(%S))", namedMember, qualifier)
            .addStatement("%T()", domain.messageCodecClassName())
            //.endControlFlow()
            .unindent()
            .add("}.%M(%T::class)\n", bindMember, DirectBufferCodec::class)

        builder.beginControlFlow("single(%M(%S))", namedMember, qualifier)
            .addStatement("%T", domain.messageConstantsClassName())
            //.endControlFlow()
            .unindent()
            .add("}.%M(%T::class)\n", bindMember, MessageMetaProvider::class)

        builder.endControlFlow()

        builder.beginControlFlow("module")
        builder.addStatement("includes(domainModule)")
        builder.endControlFlow()

        // end dsl
        builder.unindent().add("}")
        return builder.build()
    }

    companion object {
        val moduleMember = MemberName("org.koin.dsl", "module")
    }
}