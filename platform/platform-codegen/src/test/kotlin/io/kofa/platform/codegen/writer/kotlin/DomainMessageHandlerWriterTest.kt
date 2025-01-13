package io.kofa.platform.codegen.writer.kotlin

import io.kofa.platform.codegen.GeneratorTestUtils
import io.kotest.core.spec.style.FunSpec
import org.junit.jupiter.api.Assertions.*

class DomainMessageHandlerWriterTest : FunSpec({
    test("generate domain message successfully") {
        val result = DomainMessageHandlerWriter().generatedMessageHandlerFileSpec(GeneratorTestUtils.resolvedDomain)

        assertNotNull(result)
    }
})