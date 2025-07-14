package io.kofa.platform.codegen.generator

data class DomainGeneratorConfig(
    val sbeJavaOutputDir: String,
    val sbeXmlOutputDir: String,

    val domainXmlOutputDir: String = sbeXmlOutputDir,
    val generateMessageOnly: Boolean = false,
    val generateSbeOnly: Boolean = false
)