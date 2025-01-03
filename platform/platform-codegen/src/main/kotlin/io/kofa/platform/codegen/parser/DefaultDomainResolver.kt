package io.kofa.platform.codegen.parser

import arrow.atomic.AtomicInt
import io.kofa.platform.codegen.domain.*

class DefaultDomainResolver(
    private val domainProvider: () -> PlainDomain,
    private val existingDomainProvider: () -> PlainDomain?
) {

    fun resolve(): ResolvedDomain {
        val plainDomain = parseLatest()
        val existingDomain = parseExisting()

        return if (existingDomain != null) {
            checkAndMerge(plainDomain, existingDomain)
        } else {
            resolveDomain(plainDomain)
        }
    }

    private fun checkAndMerge(masterDomain: PlainDomain, generatedDomain: PlainDomain): ResolvedDomain {
        TODO()
    }

    private fun nextFieldId(fields: List<PlainDomainField>): Int {
        return fields.maxOf { field -> field.id ?: 0 } + 1
    }

    private fun resolveDomain(plainDomain: PlainDomain, updateId: Boolean = false): ResolvedDomain {
        val typeFieldIdCounter =
            plainDomain.types.associateBy({ type -> type.name }) { type -> AtomicInt(nextFieldId(type.fields)) }

        val messageIdCounter = AtomicInt(plainDomain.messages.maxOf { m -> m.id ?: 0 })

        return ResolvedDomain(
            domainName = plainDomain.domainName,
            pkgName = plainDomain.pkgName,
            imports = plainDomain.imports.map { d -> resolveDomain(d) },
            types = plainDomain.types.map { type ->
                DomainType<ResolvedDomainField>(
                    name = type.name,
                    fields = type.fields.map { f -> resolveDomainField(f, typeFieldIdCounter[type.name]!!, updateId) }
                )
            },
            enums = plainDomain.enums,
            interfaces = plainDomain.interfaces.map { type ->
                DomainInterface<ResolvedDomainField>(
                    name = type.name,
                    fields = type.fields.map { f -> resolveDomainField(f, typeFieldIdCounter[type.name]!!, updateId) }
                )
            },
            messages = plainDomain.messages.map { type ->
                DomainMessage<ResolvedDomainField>(
                    id = if (updateId && type.id == null) {
                        messageIdCounter.incrementAndGet()
                    } else {
                        type.id
                    },
                    name = type.name,
                    fields = type.fields.map { f -> resolveDomainField(f, typeFieldIdCounter[type.name]!!, updateId) }
                )
            },
        )
    }

    private fun resolveDomainField(
        plainDomainField: PlainDomainField,
        counter: AtomicInt,
        updateId: Boolean
    ): ResolvedDomainField {
        TODO()
    }

    private fun parseLatest() = domainProvider.invoke()

    private fun parseExisting() = existingDomainProvider.invoke()
}