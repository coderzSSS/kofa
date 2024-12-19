package io.kofa.platform.api.meta

import kotlin.reflect.KClass

interface MessageMetaProvider {
    fun check(eventType: Int) = tryGetMessageMeta(eventType) != null

    fun tryGetDomainClass(eventType: Int): KClass<*>?

    fun tryGetMessageType(clazz: KClass<*>): Int? = tryGetMessageMeta(clazz)?.msgType

    fun tryGetMessageMeta(clazz: KClass<*>): MessageMeta?

    fun tryGetMessageMeta(eventType: Int): MessageMeta?
}