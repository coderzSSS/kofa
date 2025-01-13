package io.kofa.platform.codegen.writer.kotlin

import io.kofa.platform.codegen.GeneratorTestUtils
import io.kotest.core.spec.style.FunSpec
import org.junit.jupiter.api.Assertions.assertNotNull

class DomainMessageConstantsWriterTest : FunSpec({
    test("generate message constants succeeds") {
        val result = DomainMessageConstantsWriter().generateMessageConstants(GeneratorTestUtils.resolvedDomain)

        assertNotNull(result)
    }
})