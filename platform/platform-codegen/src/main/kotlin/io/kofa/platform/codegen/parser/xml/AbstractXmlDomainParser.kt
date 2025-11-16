package io.kofa.platform.codegen.parser.xml

import com.google.devtools.ksp.processing.KSPLogger
import io.github.classgraph.ClassGraph
import io.kofa.platform.codegen.xsd.generated.Domain
import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.JAXBIntrospector
import java.io.File
import java.io.InputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

abstract class AbstractXmlDomainParser(
    private val logger: KSPLogger?,
    private val classpath: String? = null,
    private val rootDir: String? = null) {
    protected fun parseDomain(inputStream: InputStream, xsdFile: File? = null): Domain {
        val context = JAXBContext.newInstance(Domain::class.java.packageName, Domain::class.java.classLoader)

        val unmarshaller = context.createUnmarshaller()

        val schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)

        xsdFile?.let { xsd ->
            val schema = schemaFactory.newSchema(xsd)
            unmarshaller.schema = schema
        }

        val obj = unmarshaller.unmarshal(StreamSource(inputStream), Domain::class.java)

        return JAXBIntrospector.getValue(obj) as Domain
    }

    protected fun resolveImportUrls(import: String): List<InputStream> {
        // Split the import string by commas and trim any whitespace
        val importPaths = import.split(",").map { it.trim() }

        // Resolve each import path to a URL
        return importPaths.map(this::resolveInputStream)
    }

    fun resolveInputStream(path: String): InputStream {
        return checkNotNull(tryResolveInputStream(path)) { "Resource not found in classpath or file system: $path" }
    }

    fun resolveUrl(path: String): URL {
        return checkNotNull(tryResolveUrl(path)) { "Resource not found in classpath or file system: $path" }
    }

    fun tryResolveInputStream(path: String): InputStream? {
        return tryResolveUrl(path)?.openStream()
    }

    fun tryResolveUrl(path: String): URL? {
        if (Paths.get(path).isAbsolute) {
            return Paths.get(path).normalize().toUri().toURL()
        }

        // Try to resolve as a classpath resource first
        val classGraph = ClassGraph()
                .enableAllInfo()
                .acceptPathsNonRecursive(path)

        if (!classpath.isNullOrBlank()) {
            classGraph.overrideClasspath(classpath)
        }

        val result = classGraph.scan()
        val resource = result.getResourcesWithPathIgnoringAccept(path).firstOrNull()

        logger?.info("resolved resource $resource for $path")

        val classpathUrl = resource?.url

        return classpathUrl
            ?: // If not found in classpath, try to resolve as a file path
            try {
                val path = if (rootDir != null) Paths.get(rootDir, path) else Paths.get(path)

                if (Files.exists(path)) {
                    path.toUri().toURL()
                } else null

            } catch (_: Exception) {
                null
            }
    }
}