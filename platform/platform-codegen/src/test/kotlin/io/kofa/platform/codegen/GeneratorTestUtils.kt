package io.kofa.platform.codegen

import io.kofa.platform.codegen.parser.DefaultDomainResolver
import io.kofa.platform.codegen.parser.xml.XmlDomainParser

class GeneratorTestUtils {
    companion object {
        val rawMasterDomain = XmlDomainParser().parseRawDomain("test-master.xml")
        val masterDomain = XmlDomainParser().parse("test-master.xml")
        val resolvedDomain = DefaultDomainResolver({ masterDomain }, { null }).resolve()

        val sampleRawMasterDomain = XmlDomainParser().parseRawDomain("sample-master.xml")
        val sampleMasterDomain = XmlDomainParser().parse("sample-master.xml")
        val sampleResolvedDomain = DefaultDomainResolver({ sampleMasterDomain }, { null }).resolve()


    }
}