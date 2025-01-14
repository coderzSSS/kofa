package io.kofa.platform.codegen.domain

data class ResolvedDomain(
    val domainName: String,
    val pkgName: String,
    val version: Int = 0,
    val imports: List<ResolvedDomain>,
    val types: List<DomainType<ResolvedDomainField>>,
    val enums: List<DomainType<DomainEnumField>>,
    val interfaces : List<DomainInterface<ResolvedDomainField>>,
    val messages: List<DomainMessage<ResolvedDomainField>>
)
