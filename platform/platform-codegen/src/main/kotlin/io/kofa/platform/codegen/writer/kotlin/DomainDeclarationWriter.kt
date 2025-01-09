package io.kofa.platform.codegen.writer.kotlin

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeSpec
import io.kofa.platform.api.codec.DirectBufferCodec
import io.kofa.platform.api.dsl.DomainDeclaration
import io.kofa.platform.api.meta.MessageMetaProvider
import io.kofa.platform.codegen.domain.ResolvedDomain
import io.kofa.platform.codegen.writer.kotlin.KotlinGeneratorUtils.messageCodecClassName
import io.kofa.platform.codegen.writer.kotlin.KotlinGeneratorUtils.messageConstantsClassName

class DomainDeclarationWriter {
    fun generate(domain: ResolvedDomain): FileSpec {
        val clazzName = ClassName(domain.pkgName, domain.domainName + "DomainDeclaration");
        val fileSpecBuilder = FileSpec.builder(clazzName)
            .addType(
                TypeSpec.classBuilder(clazzName)
                    .superclass(DomainDeclaration::class)
                    .addSuperclassConstructorParameter(buildDslCodeBlock(domain))
                    .build()
            )

        return fileSpecBuilder.build()
    }

    private fun buildDslCodeBlock(domain: ResolvedDomain): CodeBlock {
        val builder = CodeBlock.builder()

        builder.addStatement("domain = %S", domain.domainName)
        builder.addStatement("pkg = %S", domain.pkgName)

        val moduleMember = MemberName("org.koin.dsl", "module")
        val bindMember = MemberName("org.koin.dsl", "bind")

        builder.beginControlFlow("val domainModule = %M", moduleMember)
        builder.beginControlFlow("factory")
            .addStatement("%T()", domain.messageCodecClassName())
            .endControlFlow()
            .add("%M(%T::class)", bindMember, DirectBufferCodec::class)

        builder.beginControlFlow("single")
            .addStatement("%T", domain.messageConstantsClassName())
            .endControlFlow()
            .add("%M(%T::class)", bindMember, MessageMetaProvider::class)

        builder.endControlFlow()

        builder.beginControlFlow("module")
        builder.addStatement("includes(domainModule)")
        builder.endControlFlow()
        return builder.build()
    }
}