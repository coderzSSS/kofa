package io.kofa.platform.core.internal.message.meta

import com.google.common.collect.HashBiMap
import io.kofa.platform.api.dsl.model.DomainMetaDefinition
import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass

internal object MessageMetaRegistry {
    private val messageMetaById = mutableMapOf<Int, MessageMeta>()
    private val messageTypeByDomainClass = HashBiMap.create<KClass<*>, Int>()
    private val messageCodecById = mutableMapOf<Int, KSerializer<*>>()

    fun loadFrom(domainMeta: DomainMetaDefinition) {
        domainMeta.messages.forEach { meta ->
            check(!messageMetaById.containsKey(meta.id)) { "conflict message id ${meta.id} found for $meta" }

            messageMetaById[meta.id] = MessageMeta(meta.id, 0, domainMeta.domain)
            messageTypeByDomainClass[meta.domainMessageClazz] = meta.id
            meta.serializer?.let {
                messageCodecById[meta.id] = it
            }
        }
    }
}