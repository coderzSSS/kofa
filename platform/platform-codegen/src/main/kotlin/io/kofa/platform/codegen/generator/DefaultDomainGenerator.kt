package io.kofa.platform.codegen.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.ksp.writeTo
import io.kofa.platform.codegen.domain.ResolvedDomain
import io.kofa.platform.codegen.writer.java.SbeCodecGenerator
import io.kofa.platform.codegen.writer.kotlin.*

class DefaultDomainGenerator(
    private val config: DomainGeneratorConfig,
    private val logger: KSPLogger,
    private val codeGenerator: CodeGenerator
) {
    fun process(domain: ResolvedDomain) {
        if (config.generateSbeOnly) {
            generateSbeOnly(domain)
            return
        } else if (config.generateMessageOnly) {
            generateMessageOnly(domain)
            return
        }

        generateMessageOnly(domain)
        generateSbeOnly(domain)

        val messageCodecFileSpec = DomainMessageCodecWriter().generate(domain)
        messageCodecFileSpec.writeTo(codeGenerator, Dependencies.ALL_FILES)
        logger.info("file ${messageCodecFileSpec.relativePath} generated")

        val domainDeclarationFileSpec = DomainDeclarationWriter().generate(domain)
        domainDeclarationFileSpec.writeTo(codeGenerator, Dependencies.ALL_FILES)
        logger.info("file ${domainDeclarationFileSpec.relativePath} generated")
    }

    private fun generateSbeOnly(domain: ResolvedDomain) {
        val generatedSbeXmlFile = SbeCodecGenerator(config.sbeJavaOutputDir, config.sbeXmlOutputDir).generate(domain)
        logger.info("generating sbe-xml file for ${domain.domainName} to ${generatedSbeXmlFile.absolutePath}")
    }

    private fun generateMessageOnly(domain: ResolvedDomain) {
        val messageFileSpec = DomainMessageWriter().generateDomainMessageFileSpec(domain)
        val messageHandlerFileSpec = DomainMessageHandlerWriter().generatedMessageHandlerFileSpec(domain)
        val messageConstantsFileSpec = DomainMessageConstantsWriter().generateMessageConstants(domain)

        messageFileSpec.writeTo(codeGenerator, Dependencies.ALL_FILES)
        logger.info("file ${messageFileSpec.relativePath} generated")

        messageHandlerFileSpec.writeTo(codeGenerator, Dependencies.ALL_FILES)
        logger.info("file ${messageHandlerFileSpec.relativePath} generated")

        messageConstantsFileSpec.writeTo(codeGenerator, Dependencies.ALL_FILES)
        logger.info("file ${messageConstantsFileSpec.relativePath} generated")
    }
}