package io.kofa.platform.codegen.writer.xml

import io.kofa.platform.codegen.domain.*
import io.kofa.platform.codegen.domain.type.DomainFieldType
import io.kofa.platform.codegen.domain.type.GeneratedFieldType
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.util.concurrent.atomic.AtomicInteger
import javax.xml.parsers.DocumentBuilderFactory

class SbeMessageXmlWriter {
    fun generateXmlContent(domain: ResolvedDomain): Document {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val document = builder.newDocument()

        val root = document.createElement("sbe:messageSchema")
        root.setAttribute("xmlns:sbe", "http://fixprotocol.io/2016/sbe")
        root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XInclude")
        root.setAttribute("package", domain.pkgName)
        root.setAttribute("id", domain.generateId().toString())
        root.setAttribute("version", domain.generateVersion().toString())
        root.setAttribute("semanticVersion", "5.2")
        root.setAttribute("description", domain.domainName)

        val types = loadDefaultSbeTypesElement()
        createSbeTypes(document, domain).forEach {
            types.appendChild(it)
        }

        root.appendChild(types)

        val messages = document.createElement("sbe:message")
        createSbeMessages(document, domain).forEach {
            messages.appendChild(it)
        }

        root.appendChild(messages)

        document.appendChild(root)

        return document
    }

    private fun loadDefaultSbeTypesElement(): Element {
        val result = this::class.java.getResourceAsStream("sbe-common-types.xml").use { stream ->
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val document = builder.parse(stream)
            document.getElementById("types")
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
            buildMessageElement(document, message)
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

    private fun buildMessageElement(document: Document, type: DomainMessage<ResolvedDomainField>): Element {
        val element = document.createElement("sbe:message")
        element.setAttribute("name", type.name)
        element.setAttribute("id", checkNotNull(type.id?.toString()) { "missing id for message ${type.name}" })

        val groupFieldIdCounter = AtomicInteger(type.fields.maxOf { field -> field.id ?: 0 })

        type.fields.forEach { field ->
            if (isNeedFlatten(field)) {
                val groupElement = document.createElement("group")
                groupElement.setAttribute("name", field.name)
                groupElement.setAttribute(
                    "id",
                    checkNotNull(field.id?.toString()) { "missing id for field ${field.name}" }
                )

                val groupFieldElements: List<Element> = (field.type as GeneratedFieldType).fields.flatMap { f ->
                    TODO()
                }
                groupFieldElements.forEach { groupElement.appendChild(it) }
                element.appendChild(groupElement)
            } else {
                val fieldElement = buildMessageFieldElement(document, field.name, field.id, field.type)
                element.appendChild(fieldElement)
            }
        }
        return element
    }

    private fun isNeedFlatten(field: ResolvedDomainField): Boolean {
        return field.type is GeneratedFieldType && !field.type.fields.all { f -> f.type.isPrimitive }
    }

    private fun buildMessageFieldElement(
        document: Document,
        fieldName: String,
        fieldId: Int?,
        fieldType: DomainFieldType
    ): Element {
        val fieldElement = document.createElement("field")
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