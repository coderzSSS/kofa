package io.kofa.platform.codegen.parser

import arrow.atomic.AtomicInt
import io.kofa.platform.codegen.domain.DomainInterface
import io.kofa.platform.codegen.domain.DomainMessage
import io.kofa.platform.codegen.domain.DomainType
import io.kofa.platform.codegen.domain.PlainDomain
import io.kofa.platform.codegen.domain.PlainDomainField
import io.kofa.platform.codegen.domain.ResolvedDomain
import io.kofa.platform.codegen.domain.ResolvedDomainField

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

    private fun resolveDomain(plainDomain: PlainDomain): ResolvedDomain {
        val typeFieldCounter = plainDomain.types.associateWith { type -> AtomicInt(nextFieldId(type.fields)) }

        return ResolvedDomain(
            domainName = plainDomain.domainName,
            pkgName = plainDomain.pkgName,
            imports = plainDomain.imports.map { d -> resolveDomain(d) },
            types = plainDomain.types.map { type ->
                DomainType<ResolvedDomainField>(
                    name = type.name,
                    fields = type.fields.map {f -> resolveDomainField(f)}
                )
            },
            enums = plainDomain.enums,
            interfaces = plainDomain.interfaces.map { type ->
                DomainInterface<ResolvedDomainField>(
                    name = type.name,
                    fields = type.fields.map {f -> resolveDomainField(f)}
                )
            },
            messages = mapOf()
        )
    }

    private fun resolveDomainField(plainDomainField: PlainDomainField): ResolvedDomainField {
        TODO()
    }

    private fun parseLatest() = domainProvider.invoke()

    private fun parseExisting() = existingDomainProvider.invoke()
}