package io.kofa.platform.codegen.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspArgs
import com.tschuchort.compiletesting.kspIncremental
import com.tschuchort.compiletesting.kspWithCompilation
import com.tschuchort.compiletesting.symbolProcessorProviders
import io.kotest.core.spec.style.FunSpec
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Assertions.*

@OptIn(ExperimentalCompilerApi::class)
class KofaDomainProcessorTest : FunSpec({
    test("smoke test") {
        val result = compilation.compile()
        result.generatedFiles.forEach { file ->
            println(file)
        }

        assertTrue(result.exitCode == KotlinCompilation.ExitCode.OK)
    }
}) {
    companion object {
        val testHandlerSource = SourceFile.kotlin(
            "TestDomainHandler.kt",
            @Language("kotlin")
            """
                package kofa.test.handler
                import org.lockfast.domain.test.generated.MsgAEvent
                
                class MyHandler {
                    fun onMyEvent(event: MsgAEvent) {
                        println(event)
                    } 
                }
            """
        )

        val testModuleSource = SourceFile.kotlin(
            "TestDomainModule.kt",
            @Language("kotlin")
            """
                package kofa.test.module
                import io.kofa.platform.api.annotation.DomainModule
                import kofa.test.handler.MyHandler
                import io.kofa.platform.api.inject.ComponentModuleDeclaration
                import org.koin.core.qualifier.named
                import org.koin.dsl.bind
                
                @DomainModule("Test", MyHandler::class)
                class MyModule {
                    fun clownConfig(): ComponentModuleDeclaration = {
                        scoped(named("limit")) {
                            100
                        }.bind()
                    }
                }
            """.trimIndent()
        )

        val compilation = KotlinCompilation().apply {
            verbose = true
            jvmTarget = "17"
            sources = listOf(testHandlerSource, testModuleSource)
            inheritClassPath = true
            kotlincArguments = listOf("-Xskip-metadata-version-check")
            kspWithCompilation = true
            kspIncremental = true
            symbolProcessorProviders = listOf(KofaDomainProcessorProvider())
            kspArgs = mutableMapOf<String, String>(
                "kofa.domain.master" to "src/test/resources/test-master.xml",
                "kofa.sbeJavaOutputDir" to "build/generated/ksp/test/java",
                "kofa.sbeXmlOutputDir" to "src/test/resources",
            )
        }
    }
}