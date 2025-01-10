package io.kofa.platform.codegen.writer.java

import io.kofa.platform.codegen.parser.DefaultDomainResolver
import io.kofa.platform.codegen.parser.xml.XmlDomainParser
import io.kotest.core.spec.style.FunSpec
import kotlin.test.assertTrue

class SbeCodecGeneratorTest : FunSpec({
    val masterDomain = XmlDomainParser().parse("test-master.xml")
    val domain = DefaultDomainResolver({ masterDomain }, { null }).resolve()

    test("sbe tool generate success") {
        val javaOutput = "build/generated/java"
        val target = SbeCodecGenerator(javaOutput, "src/test/resources")

        val result = target.generate(domain)
        assertTrue(result.isFile && result.exists() && result.length() > 0)
    }
})