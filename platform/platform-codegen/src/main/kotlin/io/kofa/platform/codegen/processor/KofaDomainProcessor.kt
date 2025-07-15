package io.kofa.platform.codegen.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ksp.writeTo
import io.github.classgraph.ClassGraph
import io.kofa.platform.api.annotation.DomainModule
import io.kofa.platform.codegen.domain.ResolvedDomain
import io.kofa.platform.codegen.generator.DefaultDomainGenerator
import io.kofa.platform.codegen.generator.DomainGeneratorConfig
import io.kofa.platform.codegen.parser.DefaultDomainResolver
import io.kofa.platform.codegen.parser.xml.XmlDomainParser
import io.kofa.platform.codegen.writer.kotlin.BusinessDeclarationWriter
import io.kofa.platform.codegen.writer.xml.GeneratedDomainXmlWriter
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.Path

class KofaDomainProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {
    private val logger = environment.logger
    private val codeGenerator = environment.codeGenerator
    private val resolvedDomain = generateDomain()

    private val componentConfigList = mutableSetOf<BusinessDeclarationWriter.ComponentTypeConfig>()

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(DomainModule::class.qualifiedName!!)

        val componentConfigs =
            symbols.filter { symbol -> symbol is KSClassDeclaration && symbol.validate() }.map { symbol ->
                val classDeclaration = symbol as KSClassDeclaration
                val moduleAnnotation =
                    classDeclaration.annotations.filter { it.annotationType.resolve().declaration.qualifiedName?.asString() == DomainModule::class.qualifiedName }
                        .single()

                symbol to BusinessDeclarationWriter.ComponentTypeConfig(
                    componentType = moduleAnnotation.arguments.first().value as String,
                    handlerClass = (moduleAnnotation.arguments.last().value as KSType).declaration as KSClassDeclaration,
                    moduleClass = classDeclaration
                )

            }.toMap()

        val validComponentConfigs = componentConfigs.values.filter { config -> config.handlerClass.validate() }

        componentConfigList.addAll(validComponentConfigs)

        val result = symbols.filterNot { it.validate() }
            .toList() + componentConfigs.filterNot { e -> e.value.handlerClass.validate() }.map { it.key }

        if (result.isEmpty() && componentConfigs.isNotEmpty()) {
            val fileSpec = BusinessDeclarationWriter(resolver).generate(resolvedDomain, componentConfigList.toList())
            fileSpec.writeTo(codeGenerator, Dependencies.ALL_FILES)
            logger.info("file ${fileSpec.relativePath} generated")
        }

        return result
    }

    private fun generateDomain(): ResolvedDomain {
        logger.info("OPTIONS: " + environment.options.toString())

        val masterDomainXmlFile =
            checkNotNull(environment.getPath("kofa.domain.master")) { "missing domain master file" }
        val generatedDomainXmlFile = environment.getPath("kofa.domain.generated")

        val scanClassPath = environment.getClassPath()

        logger.info("kofa scanning classpath: $scanClassPath")
        val xmlParser = XmlDomainParser(scanClassPath, logger)
        val domainXsd = environment.getPath("kofa.domain.xsd")?.let { xsd -> File(xmlParser.resolveUrl(xsd).file) }

        val sbeJavaOutputDir = environment.getPath("kofa.sbeJavaOutputDir", "build/generated/ksp/main/java")
        val sbeXmlOutputDir = environment.getPath("kofa.sbeXmlOutputDir", "src/main/resources")
        val domainXmlOutputDir = environment.getPath("kofa.domainXmlOutputDir", "src/main/resources")

        val generatorConfig = DomainGeneratorConfig(
            sbeJavaOutputDir = sbeJavaOutputDir!!,
            sbeXmlOutputDir = sbeXmlOutputDir!!,
            domainXmlOutputDir = domainXmlOutputDir!!
        )


        val plainDomain = xmlParser.parse(masterDomainXmlFile, domainXsd)
        val existingDomain = generatedDomainXmlFile?.let {
            xmlParser.tryResolveUrl(it)?.let { uRL ->
                xmlParser.parse(uRL.file, domainXsd)
            }
        }

        val resolvedDomain = DefaultDomainResolver({ plainDomain }, { existingDomain }).resolve()

        DefaultDomainGenerator(generatorConfig, logger, codeGenerator).process(resolvedDomain)

        val rawDomain = xmlParser.parseRawDomain(masterDomainXmlFile, domainXsd)

        val file = GeneratedDomainXmlWriter(generatorConfig.domainXmlOutputDir).writeXml(rawDomain, resolvedDomain)
        logger.info("domain generated xml file at $file")

        return resolvedDomain
    }

    private fun SymbolProcessorEnvironment.getPath(option: String, defaultValue: String? = null): String? {
        val rootDir = options["kofa.rootDir"]
        val value = options[option] ?: defaultValue

        return if (rootDir == null) value else value?.let {
            if (Path(it).isAbsolute) it else Paths.get(rootDir, it).toString()
        }
    }

    private fun SymbolProcessorEnvironment.getClassPath(): String {
        val rootDir = options["kofa.rootDir"]
        val classpath = options["kofa.classpath"]

        return buildString {
            append(".")
            if (rootDir != null) {
                append(File.pathSeparator).append(rootDir)
            }
            if (classpath != null) {
                append(File.pathSeparator).append(classpath)
            }
        }
    }
}