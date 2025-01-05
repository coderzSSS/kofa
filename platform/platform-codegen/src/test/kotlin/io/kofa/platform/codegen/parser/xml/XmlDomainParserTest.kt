package io.kofa.platform.codegen.parser.xml

import io.kotest.core.spec.style.DescribeSpec
import kotlin.test.assertNotNull

class XmlDomainParserTest: DescribeSpec({
    it("should parse a domain") {
        val parser = XmlDomainParser()
        val domain = parser.parse("test-master.xml")

        assertNotNull(domain)
    }
})