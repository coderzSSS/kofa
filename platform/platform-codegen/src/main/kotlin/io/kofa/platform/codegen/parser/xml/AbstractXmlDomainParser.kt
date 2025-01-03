package io.kofa.platform.codegen.parser.xml

import io.kofa.platform.codegen.domain.PlainDomainField
import io.kofa.platform.codegen.xsd.generated.Domain
import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.JAXBIntrospector
import java.io.File
import java.io.InputStream
import java.net.URL
import java.nio.file.Paths
import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

abstract class AbstractXmlDomainParser {
    protected fun parseDomain(inputStream: InputStream, xsdFile: File? = null): Domain {
        val context = JAXBContext.newInstance(Domain::class.java)
        val unmarshaller = context.createUnmarshaller()

        val schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)

        xsdFile?.let { xsd ->
            val schema = schemaFactory.newSchema(xsd)
            unmarshaller.schema = schema
        }

        val obj = unmarshaller.unmarshal(StreamSource(inputStream), Domain::class.java)

        return JAXBIntrospector.getValue(obj) as Domain
    }

    protected fun resolveImportUrls(import: String): List<URL> {
        // Split the import string by commas and trim any whitespace
        val importPaths = import.split(",").map { it.trim() }

        // Resolve each import path to a URL
        return importPaths.map { path ->
            // Try to resolve as a classpath resource first
            val classpathUrl = this::class.java.classLoader.getResource(path)
            classpathUrl
                ?: // If not found in classpath, try to resolve as a file path
                try {
                    Paths.get(path).toUri().toURL()
                } catch (_: Exception) {
                    throw IllegalArgumentException("Resource not found in classpath or file system: $path")
                }
        }
    }
}