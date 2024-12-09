package io.kofa.platform.core.internal.service.meta

import com.google.common.collect.HashBiMap
import io.kofa.platform.api.dsl.model.DomainMetaDefinition
import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.SerializersModule
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

    @Suppress("UNCHECKED_CAST")
    fun getMessageCodecModule() = SerializersModule {
        messageCodecById.forEach { type, serializer ->
            val clazz = getDomainClass(type)
            polymorphic(
                baseClass = Any::class,
                actualClass = clazz as KClass<Any>,
                actualSerializer = serializer as KSerializer<Any>
            )
        }
    }

    fun getDomainClass(eventType: Int) = messageTypeByDomainClass.inverse()[eventType]

    fun getMessageType(clazz: KClass<*>) = messageTypeByDomainClass[clazz]

    fun getMessageMeta(clazz: KClass<*>): MessageMeta? {
        return messageTypeByDomainClass[clazz]?.let { i -> messageMetaById[i] }
    }
}