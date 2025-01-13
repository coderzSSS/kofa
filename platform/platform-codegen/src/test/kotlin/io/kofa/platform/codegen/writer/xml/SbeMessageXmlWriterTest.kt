package io.kofa.platform.codegen.writer.xml

import io.kofa.platform.codegen.GeneratorTestUtils
import io.kotest.core.spec.style.FunSpec
import java.io.StringWriter
import kotlin.test.assertNotNull

class SbeMessageXmlWriterTest : FunSpec({
    test("generate xml success") {
        val result = SbeMessageXmlWriter().generateXmlTo(GeneratorTestUtils.resolvedDomain, StringWriter())

        val output = result.buffer.toString()
        assertNotNull(result)
    }
})