package io.kofa.platform.codegen.parser.xml

import io.kofa.platform.codegen.domain.*
import io.kofa.platform.codegen.xsd.generated.Domain
import io.kofa.platform.codegen.xsd.generated.ImplementList
import io.kofa.platform.codegen.xsd.generated.MessageField
import io.kofa.platform.codegen.xsd.generated.Type
import java.io.File
import java.io.InputStream

class XmlDomainParser(classpath: String? = null) : AbstractXmlDomainParser(classpath) {
    fun parse(xmlPath: String, xsdFile: File? = null): PlainDomain {
        return resolveInputStream(xmlPath).use {
            parse(it, xsdFile)
        }
    }

    private fun parse(inputStream: InputStream, xsdFile: File? = null): PlainDomain {
        return processDomain(parseDomain(inputStream, xsdFile))
    }

    private fun processDomain(domain: Domain): PlainDomain {
        val imports = resolveImports(domain)
        val types = resolveTypes(domain)
        val enums = resolveEnums(domain)
        val interfaces = resolveInterfaces(domain)
        val messages = resolveMessages(domain)

        val shallowDomain = PlainDomain(
            domainName = domain.name,
            pkgName = domain.`package`,
            imports = imports,
            types = types,
            enums = enums,
            interfaces = interfaces,
            messages = messages
        )

        return checkAndFlattenDomain(shallowDomain, domain)
    }

    private fun checkAndFlattenDomain(parsedDomain: PlainDomain, originDomain: Domain): PlainDomain {
        return PlainDomain(
            domainName = parsedDomain.domainName,
            pkgName = parsedDomain.pkgName,
            imports = parsedDomain.imports,
            types = resolveTypes(originDomain, parsedDomain),
            enums = parsedDomain.enums,
            interfaces = resolveInterfaces(originDomain, parsedDomain),
            messages = resolveMessages(originDomain, parsedDomain)
        )
    }

    private fun resolveImports(domain: Domain): List<PlainDomain> {
        if (domain.import.isNullOrBlank()) {
            return listOf()
        }

        return resolveImportUrls(domain.import).map { ins ->
            ins.use { stream ->
                parse(stream)
            }
        }
    }

    private fun resolveTypes(domain: Domain, shallowDomain: PlainDomain? = null): List<DomainType<PlainDomainField>> {
        if (domain.types == null) {
            return listOf()
        }

        return domain.types.typeOrComposite.map { type ->
            DomainType(
                name = type.name,
                fields = type.fields.field.map { field ->
                    PlainDomainField(
                        id = field.id?.toInt(),
                        name = field.name,
                        typeName = field.type,
                        length = field.maxLength?.toInt(),
                        deprecated = field.isDeprecated ?: false
                    )
                } + resolveExtension(shallowDomain, (type as? Type)?.implements)
            )
        }
    }

    private fun resolveEnums(domain: Domain): List<DomainType<DomainEnumField>> {
        if (domain.enums == null) {
            return listOf()
        }

        return domain.enums.enumOrEnum16.map { type ->
            val value = type.value
            DomainType(
                name = value.name,
                fields = value.item.map { field ->
                    DomainEnumField(
                        name = field.name,
                        value = field.value?.toInt(),
                        deprecated = field.isDeprecated ?: false
                    )
                }
            )
        }
    }

    private fun resolveMessages(
        domain: Domain,
        shallowDomain: PlainDomain? = null
    ): List<DomainMessage<PlainDomainField>> {
        if (domain.messages == null) {
            return listOf()
        }

        return domain.messages.message.map { type ->
            DomainMessage(
                name = type.name,
                fields = type.fields.fieldOrUniqueIdField.map { field ->
                    PlainDomainField(
                        id = field.id?.toInt(),
                        name = field.name,
                        typeName = field.type,
                        length = (field as? MessageField)?.maxLength?.toInt(),
                        deprecated = field.isDeprecated ?: false
                    )
                } + resolveExtension(shallowDomain, type.implements)
            )
        }
    }

    private fun resolveInterfaces(
        domain: Domain,
        shallowDomain: PlainDomain? = null
    ): List<DomainInterface<PlainDomainField>> {
        if (domain.interfaces == null) {
            return listOf()
        }

        return domain.interfaces.`interface`.map { type ->
            DomainInterface(
                name = type.name,
                fields = type.fields.field.map { field ->
                    PlainDomainField(
                        name = field.name,
                        typeName = field.type,
                        length = field.maxLength?.toInt(),
                        deprecated = field.isDeprecated ?: false
                    )
                } + resolveExtension(shallowDomain, type.implements)
            )
        }
    }

    private fun resolveExtension(shallowDomain: PlainDomain?, implements: ImplementList?): List<PlainDomainField> {
        if (shallowDomain == null || implements == null || implements.`interface`.isEmpty()) {
            return listOf()
        }

        return resolveExtension(shallowDomain, implements.`interface`.map { i -> i.name })
    }

    private fun resolveExtension(shallowDomain: PlainDomain, names: List<String>): List<PlainDomainField> {
        var fields =
            shallowDomain.interfaces.filter { type -> names.contains(type.name) }.flatMap { type -> type.fields }

        if (fields.isEmpty()) {
            fields = shallowDomain.imports.flatMap { d -> resolveExtension(d, names) }
        }

        return fields.associateBy { field -> field.name }.values.toList()
    }

    private operator fun List<PlainDomainField>.plus(values: List<PlainDomainField>): List<PlainDomainField> {
        return buildList {
            addAll(this@plus)
            addAll(values.filter { v1 -> this@plus.none { v2 -> v1.name == v2.name } })
        }
    }
}