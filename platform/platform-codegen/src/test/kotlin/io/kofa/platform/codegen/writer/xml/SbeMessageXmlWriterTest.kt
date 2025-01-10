package io.kofa.platform.codegen.writer.xml

import io.kofa.platform.codegen.parser.DefaultDomainResolver
import io.kofa.platform.codegen.parser.xml.XmlDomainParser
import io.kotest.core.spec.style.FunSpec
import java.io.StringWriter
import kotlin.test.assertNotNull

class SbeMessageXmlWriterTest : FunSpec({
    val masterDomain = XmlDomainParser().parse("test-master.xml")
    val domain = DefaultDomainResolver({ masterDomain }, { null }).resolve()

    test("generate xml success") {
        val result = SbeMessageXmlWriter().generateXmlTo(domain, StringWriter())

        val output = result.buffer.toString()
        assertNotNull(result)
    }
})