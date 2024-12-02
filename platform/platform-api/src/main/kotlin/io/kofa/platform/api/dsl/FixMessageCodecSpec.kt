package io.kofa.platform.api.dsl

import io.kofa.platform.api.annotation.KofaDsl
import io.kofa.platform.api.dsl.model.FixDecoder
import io.kofa.platform.api.dsl.model.FixEncoder
import kotlin.reflect.KProperty1

@KofaDsl
interface FixMessageCodecSpec<T : Any> {
    fun mapping(field: KProperty1<T, String>, tag: String) = mapping(field, tag, { s -> s }, { s -> s })

    fun <V: Any> mapping(
        field: KProperty1<T, V>,
        tag: String,
        encoder: FixEncoder<V>? = null,
        decoder: FixDecoder<V>? = null
    )
}