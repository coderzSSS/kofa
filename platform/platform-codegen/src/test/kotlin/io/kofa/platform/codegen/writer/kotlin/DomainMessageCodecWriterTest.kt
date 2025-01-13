package io.kofa.platform.codegen.writer.kotlin

import io.kofa.platform.codegen.GeneratorTestUtils
import io.kotest.core.spec.style.FunSpec
import org.junit.jupiter.api.Assertions.*

class DomainMessageCodecWriterTest : FunSpec({
    test("generate message codec succeeds") {
        val result = DomainMessageCodecWriter().generate(GeneratorTestUtils.resolvedDomain)

        assertNotNull(result)
    }
})