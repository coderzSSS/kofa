package io.kofa.platform.codegen.writer.xml

import io.kofa.platform.codegen.domain.ResolvedDomain
import io.kofa.platform.codegen.xsd.generated.Composite
import io.kofa.platform.codegen.xsd.generated.Domain
import io.kofa.platform.codegen.xsd.generated.Enum
import io.kofa.platform.codegen.xsd.generated.EnumField
import io.kofa.platform.codegen.xsd.generated.EnumList
import io.kofa.platform.codegen.xsd.generated.Message
import io.kofa.platform.codegen.xsd.generated.MessageField
import io.kofa.platform.codegen.xsd.generated.MessageFieldList
import io.kofa.platform.codegen.xsd.generated.MessageList
import io.kofa.platform.codegen.xsd.generated.ObjectFactory
import io.kofa.platform.codegen.xsd.generated.Type
import io.kofa.platform.codegen.xsd.generated.TypeField
import io.kofa.platform.codegen.xsd.generated.TypeFieldList
import io.kofa.platform.codegen.xsd.generated.TypeList
import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.JAXBElement
import jakarta.xml.bind.Marshaller
import java.io.File
import java.io.FileOutputStream
import java.math.BigInteger
import javax.xml.namespace.QName

class GeneratedDomainXmlWriter(private val outputDir: String) {
    fun writeXml(rawDomain: Domain, domain: ResolvedDomain): File {
        val generatedDomain = convert(domain)

        val file = File("$outputDir/${rawDomain.name}-generated.xml")

        writeDomain(generatedDomain, file)

        return file
    }

    private fun writeDomain(domain: Domain, file: File) {
        val context = JAXBContext.newInstance(Domain::class.java.packageName, Domain::class.java.classLoader)
        val marshaller = context.createMarshaller()

        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)

        val jaxbDomain = ObjectFactory().createDomain(domain)
        marshaller.marshal(jaxbDomain, FileOutputStream(file))
    }

    private fun convert(domain: ResolvedDomain): Domain {
        val result = Domain()

        result.name = domain.domainName
        result.`package` = domain.pkgName

        populateEnum(domain, result)
        populateType(domain, result)
        populateMessage(domain, result)

        return result
    }

    private fun populateEnum(domain: ResolvedDomain, result: Domain){
        val enums = domain.enums.map { enum ->
            val enumType = Enum()
            enumType.name = enum.name
            val enumFields = enum.fields.map { field ->
                val enumField = EnumField()
                enumField.name = field.name
                enumField.value = field.value ?.let { BigInteger.valueOf(it.toLong()) }

                enumField
            }

            enumType.item.addAll(enumFields)

            enumType
        }

        val enumList = EnumList()
        val jaxbElements = enums.map { enum ->
            val qname = if (enum.item.any { it.value != null && it.value.toLong() > 256 }) "enum" else "enum16"
            JAXBElement<Enum>(QName(qname), Enum::class.java, enum)
        }

        enumList.enumOrEnum16.addAll(jaxbElements)

        result.enums = enumList
    }

    private fun populateType(domain: ResolvedDomain, result: Domain){
        val typeList = TypeList()

        val types = domain.types.map { type ->
            val jaxbType = Type()
            jaxbType.name = type.name
            val typeFieldList = TypeFieldList()

            jaxbType.fields = typeFieldList

            val typeFields = type.fields.map { field ->
                val jaxbField = TypeField()
                jaxbField.name = field.name
                jaxbField.id = field.id?.let { BigInteger.valueOf(it.toLong()) }
                jaxbField.type = field.type.typeName

                jaxbField
            }

            typeFieldList.field.addAll(typeFields)

            jaxbType
        }

        typeList.typeOrComposite.addAll(types)

        result.types = typeList
    }

    private fun populateMessage(domain: ResolvedDomain, result: Domain){
        val messageList = MessageList()

        val messages = domain.messages.map { message ->
            val jaxbType = Message()
            jaxbType.name = message.name
            jaxbType.id = message.id?.let { BigInteger.valueOf(it.toLong()) }

            val messageFieldList = MessageFieldList()

            jaxbType.fields = messageFieldList

            val messageFields = message.fields.map { field ->
                val jaxbField = MessageField()
                jaxbField.name = field.name
                jaxbField.id = field.id?.let { BigInteger.valueOf(it.toLong()) }
                jaxbField.type = field.type.typeName

                jaxbField
            }

            messageFieldList.fieldOrUniqueIdField.addAll(messageFields)

            jaxbType
        }

        messageList.message.addAll(messages)

        result.messages = messageList
    }
}