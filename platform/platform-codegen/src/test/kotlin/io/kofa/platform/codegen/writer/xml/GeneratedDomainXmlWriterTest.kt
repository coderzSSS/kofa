package io.kofa.platform.codegen.writer.xml

import io.kofa.platform.codegen.GeneratorTestUtils
import io.kotest.core.spec.style.FunSpec
import java.io.StringWriter
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GeneratedDomainXmlWriterTest : FunSpec({
    test("generate xml success") {
        val result = GeneratedDomainXmlWriter("src/test/resources")
            .writeXml(GeneratorTestUtils.rawMasterDomain, GeneratorTestUtils.resolvedDomain)

        assertTrue(result.exists())
    }
})