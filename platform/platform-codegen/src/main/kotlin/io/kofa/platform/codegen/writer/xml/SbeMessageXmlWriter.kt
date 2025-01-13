package io.kofa.platform.codegen.writer.xml

import io.kofa.platform.codegen.domain.*
import io.kofa.platform.codegen.domain.type.DomainFieldType
import io.kofa.platform.codegen.domain.type.GeneratedFieldType
import io.kofa.platform.codegen.writer.kotlin.KotlinGeneratorUtils.flattenFieldName
import io.kofa.platform.codegen.writer.kotlin.KotlinGeneratorUtils.isNeedFlatten
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.Writer
import java.util.concurrent.atomic.AtomicInteger
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class SbeMessageXmlWriter {
    fun <T : Writer> generateXmlTo(domain: ResolvedDomain, writer: T): T {
        val doc = generateXmlDocument(domain)
        val tf = TransformerFactory.newInstance()
        val transformer = tf.newTransformer()
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.transform(DOMSource(doc), StreamResult(writer))

        return writer
    }

    fun generateXmlDocument(domain: ResolvedDomain): Document {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val document = builder.newDocument()

        val root = document.createElement("sbe:messageSchema")
        root.setAttribute("xmlns:sbe", "http://fixprotocol.io/2016/sbe")
        root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XInclude")
        root.setAttribute("package", domain.pkgName + ".sbe")
        root.setAttribute("id", domain.generateId().toString())
        root.setAttribute("version", domain.generateVersion().toString())
        root.setAttribute("semanticVersion", "5.2")
        root.setAttribute("description", domain.domainName)

        val types = document.importNode(loadDefaultSbeTypesElement(), true)

        createSbeTypes(document, domain).forEach {
            types.appendChild(it)
        }

        root.appendChild(types)

        createSbeMessages(document, domain).forEach {
            root.appendChild(it)
        }

        document.appendChild(root)

        return document
    }

    private fun loadDefaultSbeTypesElement(): Element {
        val result = this::class.java.classLoader.getResourceAsStream("sbe-common-types.xml")?.use { stream ->
            val factory = DocumentBuilderFactory.newInstance()
            factory.isIgnoringElementContentWhitespace = true

            val builder = factory.newDocumentBuilder()
            val document = builder.parse(stream)

            document.documentElement
        }

        return requireNotNull(result) { "no common sbe types loaded" }
    }

    private fun createSbeTypes(document: Document, domain: ResolvedDomain): List<Element> {
        return domain.enums.map { type ->
            buildEnumElement(document, type)
        } + domain.types.mapNotNull { type ->
            buildTypeElement(document, type)
        }
    }

    private fun createSbeMessages(document: Document, domain: ResolvedDomain): List<Element> {
        return domain.messages.map { message ->
            buildMessageElement(domain, document, message)
        }
    }

    private fun buildEnumElement(document: Document, type: DomainType<DomainEnumField>): Element {
        val element = document.createElement("enum")
        element.setAttribute("name", type.name)
        val encodingType = if (type.fields.maxOf { field -> field.value ?: 0 } > 256) {
            "int16"
        } else {
            "int8"
        }

        element.setAttribute("encodingType", encodingType)
        type.fields.forEach { field ->
            val fieldElement = document.createElement("validValue")
            fieldElement.setAttribute("name", field.name)
            fieldElement.textContent = field.value.toString()
            element.appendChild(fieldElement)
        }

        return element
    }

    private fun buildTypeElement(document: Document, type: DomainType<ResolvedDomainField>): Element? {
        val isComposite = type.fields.all { field -> field.type.isPrimitive }

        if (isComposite) {
            val element = document.createElement("composite")
            element.setAttribute("name", type.name)
            type.fields.forEach { field ->
                val fieldElement = document.createElement("type")
                fieldElement.setAttribute("name", field.name)
                fieldElement.setAttribute("primitiveType", field.type.sbeType)
            }
            return element
        }

        return null
    }

    private fun buildMessageElement(
        domain: ResolvedDomain,
        document: Document,
        type: DomainMessage<ResolvedDomainField>
    ): Element {
        val element = document.createElement("sbe:message")
        element.setAttribute("name", type.name)
        element.setAttribute("id", checkNotNull(type.id?.toString()) { "missing id for message ${type.name}" })

        val groupFieldIdCounter = AtomicInteger(type.fields.maxOf { field -> field.id ?: 0 })

        type.fields.sortedBy { field -> sortFieldType(field.type) }.forEach { field ->
            if (isNeedFlatten(field.type)) {
                val groupElement = document.createElement("group")
                groupElement.setAttribute("name", field.name)
                groupElement.setAttribute(
                    "id",
                    checkNotNull(field.id?.toString()) { "missing id for field ${field.name} from ${type.name}" }
                )

                val groupFieldElements: List<Element> =
                    flattenGroupField(
                        domain,
                        document,
                        groupFieldIdCounter.get(),
                        groupFieldIdCounter,
                        field.name,
                        getDomainTypeField(domain, type.name, field.name)
                    )

                groupFieldElements.forEach { groupElement.appendChild(it) }
                element.appendChild(groupElement)
            } else {
                val fieldElement = buildMessageFieldElement(document, field.name, field.id, field.type)
                element.appendChild(fieldElement)
            }
        }
        return element
    }

    private fun sortFieldType(type: DomainFieldType): Int {
        return if (type.isPrimitive || type.isEnum || type.isBoolean || type.isFixed) {
            0
        } else if (isNeedFlatten(type)){
            1
        } else {
            2
        }
    }

    private fun getDomainTypeField(
        domain: ResolvedDomain,
        typeName: String,
        typeFieldName: String
    ): ResolvedDomainField {
        return domain.types.singleOrNull { type -> type.name == typeName }?.fields?.singleOrNull { field -> field.name == typeFieldName }
            ?: domain.messages.singleOrNull { type -> type.name == typeName }?.fields?.singleOrNull { field -> field.name == typeFieldName }
            ?: throw java.lang.IllegalStateException("cannot found field definition for $typeName -> $typeFieldName")
    }

    private fun flattenGroupField(
        domain: ResolvedDomain,
        document: Document,
        initId: Int,
        groupIdCounter: AtomicInteger,
        prefix: String,
        field: ResolvedDomainField
    ): List<Element> {
        return if (isNeedFlatten(field.type)) {
            (field.type as GeneratedFieldType).fields.entries.sortedBy { entry -> sortFieldType(entry.value) }.flatMap { entry ->
                flattenGroupField(
                    domain,
                    document,
                    initId,
                    groupIdCounter,
                    flattenFieldName(prefix, field.type.typeName),
                    getDomainTypeField(domain, field.type.typeName, entry.key)
                )
            }
        } else {
            val id = initId + checkNotNull(field.id) { "missing field id for $field" }
            if (id > groupIdCounter.get()) {
                groupIdCounter.set(id)
            }

            listOf(
                buildMessageFieldElement(
                    document,
                    flattenFieldName(prefix, field.name),
                    id,
                    field.type
                )
            )
        }
    }

    private fun buildMessageFieldElement(
        document: Document,
        fieldName: String,
        fieldId: Int?,
        fieldType: DomainFieldType
    ): Element {
        val elementName = if (fieldType.isPrimitive || fieldType.isEnum || fieldType.isBoolean || fieldType.isFixed) {
            "field"
        } else {
            "data"
        }

        val fieldElement = document.createElement(elementName)
        fieldElement.setAttribute("name", fieldName)
        fieldElement.setAttribute("id", checkNotNull(fieldId?.toString()) { "missing id for field $fieldName" })
        if (fieldType.isSbeType) {
            fieldElement.setAttribute("type", fieldType.sbeType)
        } else if (fieldType.isEnum) {
            fieldElement.setAttribute("type", fieldType.typeName)
        } else {
            throw IllegalStateException("cannot handle field $fieldName type $fieldType")
        }

        return fieldElement
    }

    private fun ResolvedDomain.generateId(): Int {
        return domainName.chars().sum()
    }

    private fun ResolvedDomain.generateVersion() = 0
}