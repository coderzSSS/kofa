package io.kofa.platform.api.dsl.builder

import io.kofa.platform.api.dsl.FixMessageCodecSpec
import io.kofa.platform.api.dsl.model.FixDecoder
import io.kofa.platform.api.dsl.model.FixEncoder
import io.kofa.platform.api.dsl.model.FixMessageCodecMetaDefinition
import io.kofa.platform.api.dsl.model.FixMessageFieldCodecMetaDefinition
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

class FixMessageCodecSpecBuilder<T : Any> : FixMessageCodecSpec<T> {
    private val codecs: MutableMap<KProperty1<T, *>, MutableMap<String, MutableList<FixMessageFieldCodecMetaDefinition<T, *>>>> =
        mutableMapOf()

    override fun <V : Any> mapping(
        field: KProperty1<T, V>,
        tag: String,
        encoder: FixEncoder<V>?,
        decoder: FixDecoder<V>?
    ) {
        check(!codecs.containsKey(field)) { "fix mapping already specified for field $field" }

        val codecByTag = codecs.computeIfAbsent(field) { mutableMapOf() }
        check(!codecByTag.containsKey(tag)) { "fix tag {$tag} already specified for field $field" }

        codecByTag.computeIfAbsent(tag) { mutableListOf() }.add(
            FixMessageFieldCodecMetaDefinition<T, V>(
                tag,
                field,
                decoder,
                encoder
            )
        )
    }

    internal fun build(fixMessageClazz: KClass<T>): FixMessageCodecMetaDefinition<T> {
        return FixMessageCodecMetaDefinition(
            fixMessageClazz,
            codecs.flatMap { e -> e.value.flatMap { e2 -> e2.value } }
        )
    }
}