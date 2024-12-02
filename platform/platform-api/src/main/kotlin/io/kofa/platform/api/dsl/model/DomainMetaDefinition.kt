package io.kofa.platform.api.dsl.model

import org.koin.dsl.ModuleDeclaration

data class DomainMetaDefinition (
    val pkg: String,
    val domain: String,
    val imports: List<String>,
    val modules: List<ModuleDeclaration>,
    val messages: List<MessageMetaDefinition<*>>,
    val fixCodecs: List<FixMessageCodecMetaDefinition<*>>
)