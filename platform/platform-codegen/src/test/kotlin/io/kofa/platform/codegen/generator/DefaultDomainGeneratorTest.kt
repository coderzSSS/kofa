package io.kofa.platform.codegen.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import io.kofa.platform.codegen.GeneratorTestUtils
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import java.io.FileOutputStream
import java.nio.file.Paths

class DefaultDomainGeneratorTest : FunSpec({
    test("domain generation succeeds") {
        val config = DomainGeneratorConfig(
            sbeJavaOutputDir = "build/generated/java",
            sbeXmlOutputDir = "src/test/resources",
        )

        val kLogger = mockk<KSPLogger>()
        val codeGenerator = mockk<CodeGenerator>()

        every { kLogger.info(any(), any()) } answers { println(arg<String>(0)) }
        every { codeGenerator.createNewFile(any(), any(), any()) } answers {
            val fileName = buildList<String> {
                addAll(arg<String>(1).split('.'))
                add(arg<String>(2) + ".kt")
            }
            val file = Paths.get("build/generated/kotlin", *fileName.toTypedArray()).toFile()

            file.parentFile.mkdirs()
            file.createNewFile()
            FileOutputStream(file)
        }

        val generator = DefaultDomainGenerator(config, kLogger, codeGenerator)

        generator.process(GeneratorTestUtils.resolvedDomain)
    }
})