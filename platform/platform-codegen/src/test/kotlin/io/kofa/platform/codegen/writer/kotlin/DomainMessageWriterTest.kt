package io.kofa.platform.codegen.writer.kotlin

import io.kofa.platform.codegen.GeneratorTestUtils
import io.kotest.core.spec.style.FunSpec
import org.junit.jupiter.api.Assertions.*

class DomainMessageWriterTest : FunSpec({
    test("test message generated successfully") {
        val result = DomainMessageWriter().generateDomainMessageFileSpec(GeneratorTestUtils.resolvedDomain)

        assertNotNull(result)
    }
})