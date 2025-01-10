package io.kofa.platform.codegen.writer.java

import io.kofa.platform.codegen.domain.ResolvedDomain
import io.kofa.platform.codegen.writer.xml.SbeMessageXmlWriter
import uk.co.real_logic.sbe.SbeTool
import uk.co.real_logic.sbe.xml.IrGenerator
import uk.co.real_logic.sbe.xml.SchemaTransformerFactory
import java.io.File
import java.io.FileWriter
import java.nio.file.Paths

class SbeCodecGenerator(private val javaOutputDir: String, private val xmlOutputDir: String) {
    fun generate(domain: ResolvedDomain): File {
        val xmlFile = Paths.get(xmlOutputDir, "${domain.domainName}-sbe-generated.xml").toFile()
        val writer = SbeMessageXmlWriter().generateXmlTo(domain, FileWriter(xmlFile))
        writer.flush()
        writer.close()


        val schema = SbeTool.parseSchema(xmlFile.path)
        val transformer = SchemaTransformerFactory(null)

        val ir = IrGenerator().generate(transformer.transform(schema), null)

        SbeTool.generate(ir, javaOutputDir, "java")

        return xmlFile
    }
}