package io.kofa.platform.api.dsl.model

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

typealias FixDecoder<T> = (String) -> T
typealias FixEncoder<T> = (T) -> String

data class FixMessageCodecMetaDefinition<T: Any> (
    val fixMessageClazz: KClass<T>,
    val codecs: List<FixMessageFieldCodecMetaDefinition<T, *>>
)

data class FixMessageFieldCodecMetaDefinition<T: Any, V: Any>(
    val tag: String,
    val field: KProperty1<T, V>,
    val decoder: FixDecoder<V>? = null,
    val encoder: FixEncoder<V>? = null
)