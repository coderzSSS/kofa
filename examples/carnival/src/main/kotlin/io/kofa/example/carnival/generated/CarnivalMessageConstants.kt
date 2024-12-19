package io.kofa.example.carnival.generated

import io.kofa.example.carnival.domain.message.CarnivalEvent.Apple
import io.kofa.example.carnival.domain.message.CarnivalEvent.Banana
import io.kofa.platform.api.meta.MessageMeta
import io.kofa.platform.api.meta.MessageMetaProvider
import kotlin.reflect.KClass

object CarnivalMessageConstants : MessageMetaProvider {
    val META_BANANA = MessageMeta(3, 0, "Carnival")
    val META_APPLE = MessageMeta(4, 0, "Carnival")

    override fun tryGetDomainClass(eventType: Int): KClass<*>? {
        return when (eventType) {
            3 -> Banana::class
            4 -> Apple::class
            else -> null
        }
    }

    override fun tryGetMessageMeta(clazz: KClass<*>): MessageMeta? {
        return when (clazz) {
            Banana::class -> META_BANANA
            Apple::class -> META_APPLE
            else -> null
        }
    }

    override fun tryGetMessageMeta(eventType: Int): MessageMeta? {
        return when (eventType) {
            3 -> META_BANANA
            4 -> META_APPLE
            else -> null
        }
    }
}