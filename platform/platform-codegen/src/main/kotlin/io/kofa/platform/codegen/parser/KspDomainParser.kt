package io.kofa.platform.codegen.parser

import com.google.devtools.ksp.symbol.KSClassDeclaration
import io.kofa.platform.codegen.domain.PlainDomain
import io.kofa.platform.codegen.domain.type.DomainFieldType

class KspDomainParser {
    fun parse(domainClass: KSClassDeclaration, domainMessageClasses: List<KSClassDeclaration>): PlainDomain {
        TODO()
    }

    fun resolveType(ksClass: KSClassDeclaration): DomainFieldType {
        TODO()
    }
}