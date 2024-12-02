package io.kofa.platform.api.dsl.builder

import io.kofa.platform.api.dsl.DomainSpec
import io.kofa.platform.api.dsl.FixMessageCodecSpec
import io.kofa.platform.api.dsl.model.DomainMetaDefinition
import io.kofa.platform.api.dsl.model.FixMessageCodecMetaDefinition
import io.kofa.platform.api.dsl.model.MessageMetaDefinition
import kotlinx.serialization.KSerializer
import org.koin.core.module.Module
import org.koin.dsl.ModuleDeclaration
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass

class DomainSpecBuilder : DomainSpec {
    override lateinit var domain: String

    override lateinit var pkg: String

    private val modules = mutableSetOf<ModuleDeclaration>()
    private val imports = mutableListOf<String>()
    private val messages: MutableMap<KClass<*>, MessageMetaDefinition<*>> = mutableMapOf()
    private val fixCodecs: MutableMap<KClass<*>, FixMessageCodecMetaDefinition<*>> = mutableMapOf()

    override fun domain(domainClazz: KClass<*>) {
        TODO("Not yet implemented")
    }

    override fun import(pkg: String, domain: String) {
        this.imports.add("$pkg.$domain")
    }

    override fun module(module: Module.() -> Unit) {
        this.modules.add(module)
    }

    override fun <T : Any> message(id: Int, msgClass: KClass<T>, name: String?) {
        check(!messages.containsKey(msgClass)) { "message type $msgClass already exists" }

        this.messages.computeIfAbsent(msgClass) {
            MessageMetaDefinition(id, msgClass)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> codec(
        domainClazz: KClass<T>,
        serializer: KSerializer<T>
    ) {
        check(messages.containsKey(domainClazz)) { "message type $domainClazz does not exist" }
        val meta = this.messages[domainClazz] as MessageMetaDefinition<T>

        this.messages.put(domainClazz, meta.copy(serializer = serializer))
    }

    override fun <T : Any> fixCodec(
        msgClass: KClass<T>,
        codecAction: FixMessageCodecSpec<T>.() -> Unit
    ) {
        check(!this.fixCodecs.containsKey(msgClass)) { "fix message type $msgClass already exists" }

        val builder = FixMessageCodecSpecBuilder<T>()
        codecAction.invoke(builder)

        val codec = builder.build(msgClass)
        this.fixCodecs.put(msgClass, codec)
    }

//    @Suppress("UNCHECKED_CAST")
//    private fun serializersModule(): SerializersModule {
//        return SerializersModule {
//            messages.forEach {
//                polymorphic(
//                    baseClass = Any::class,
//                    actualClass = it.key as KClass<Any>,
//                    it.value.serializer as KSerializer<Any>
//                )
//            }
//        }
//    }

    internal fun build(): DomainMetaDefinition {
        return DomainMetaDefinition(
            this.pkg,
            this.domain,
            this.imports,
            this.modules.toList(),
            this.messages.values.toList(),
            this.fixCodecs.values.toList()
        )
    }

}