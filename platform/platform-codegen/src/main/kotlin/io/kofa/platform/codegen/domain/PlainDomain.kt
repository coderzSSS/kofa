package io.kofa.platform.codegen.domain

data class PlainDomain(
    val domainName: String,
    val pkgName: String,
    val imports: List<PlainDomain>,
    val types: List<DomainType<PlainDomainField>>,
    val enums : List<DomainType<DomainEnumField>>,
    val interfaces : List<DomainInterface<PlainDomainField>>,
    val messages: List<DomainMessage<PlainDomainField>>
)
