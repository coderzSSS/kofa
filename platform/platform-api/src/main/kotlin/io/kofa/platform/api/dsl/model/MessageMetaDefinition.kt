package io.kofa.platform.api.dsl.model

import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass

data class MessageMetaDefinition<D: Any> (
    val id: Int,
    val domainMessageClazz: KClass<out D>,
    val serializer: KSerializer<D>? = null
)