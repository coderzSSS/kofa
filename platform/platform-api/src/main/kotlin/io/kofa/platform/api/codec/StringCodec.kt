package io.kofa.platform.api.codec

import kotlin.reflect.KClass

interface StringCodec {
    fun <T> encodeToStringBuilder(value: T, builder: StringBuilder)

    fun <T: Any> decodeFromString(clazz: KClass<T>, value: String): T
}