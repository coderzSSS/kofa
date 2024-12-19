package io.kofa.platform.core.internal.service.meta

import com.google.common.collect.HashBiMap
import io.kofa.platform.api.dsl.model.DomainMetaDefinition
import io.kofa.platform.api.meta.MessageMeta
import io.kofa.platform.api.meta.MessageMetaProvider
import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.SerializersModule
import kotlin.reflect.KClass

internal object MessageMetaRegistry {
    private val messageMetaById = mutableMapOf<Int, MessageMeta>()
    private val messageTypeByDomainClass = HashBiMap.create<KClass<*>, Int>()
    private val messageCodecById = mutableMapOf<Int, KSerializer<*>>()
    private var delegates = mutableSetOf<MessageMetaProvider>();

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

    fun addProviders(providers: Collection<MessageMetaProvider>) {
        delegates.addAll(providers)
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

    fun getDomainClass(eventType: Int) : KClass<*>? {
        val result = delegates.firstNotNullOfOrNull { p -> p.tryGetDomainClass(eventType) }
        return result ?: messageTypeByDomainClass.inverse()[eventType]
    }

    fun getMessageType(clazz: KClass<*>) : Int? {
        val result = delegates.firstNotNullOfOrNull { p -> p.tryGetMessageType(clazz) }
        return result ?: messageTypeByDomainClass[clazz]
    }

    fun getMessageMeta(clazz: KClass<*>): MessageMeta? {
        val result = delegates.firstNotNullOfOrNull { p -> p.tryGetMessageMeta(clazz) }
        return result ?: messageTypeByDomainClass[clazz]?.let { i -> messageMetaById[i] }
    }
}