package io.kofa.platform.codegen.writer.java

import io.kofa.platform.codegen.GeneratorTestUtils
import io.kotest.core.spec.style.FunSpec
import kotlin.test.assertTrue

class SbeCodecGeneratorTest : FunSpec({
    test("sbe tool generate success") {
        val javaOutput = "build/generated/java"
        val target = SbeCodecGenerator(javaOutput, "src/test/resources")

        val result = target.generate(GeneratorTestUtils.resolvedDomain)
        assertTrue(result.isFile && result.exists() && result.length() > 0)
    }
})