package io.kofa.platform.codegen.parser.xml

import io.github.classgraph.ClassGraph
import io.kotest.core.spec.style.DescribeSpec
import kotlin.test.assertNotNull

class XmlDomainParserTest: DescribeSpec({
    it("should parse a domain") {
        val parser = XmlDomainParser()
        val domain = parser.parse("test-master.xml")

        assertNotNull(domain)
    }
    xit("should resolve from classpath") {
        val parser = XmlDomainParser()

        val result = parser.resolveUrl("platform-master.xml")
        assertNotNull(result)
    }
})