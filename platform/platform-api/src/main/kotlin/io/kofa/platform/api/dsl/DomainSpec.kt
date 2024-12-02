package io.kofa.platform.api.dsl

import io.kofa.platform.api.annotation.KofaDsl
import kotlinx.serialization.KSerializer
import org.koin.dsl.ModuleDeclaration
import kotlin.reflect.KClass

@KofaDsl
interface DomainSpec {
    var domain: String
    var pkg: String

    fun import(pkg: String, domain: String)

    fun module(module: ModuleDeclaration)

    fun domain(domainClazz: KClass<*>)

    fun <T : Any> domain(domainClass: KClass<T>, pkg: String? = null, domain: String? = null) {
        domain(domainClass);

        this.domain = domain ?: domainClass.simpleName!!
        this.pkg = pkg ?: domainClass.qualifiedName!!
    }

    fun <T : Any> message(id: Int, msgClass: KClass<T>, name: String? = null)

    fun <T : Any> message(id: Int, msgClass: KClass<T>, serializer: KSerializer<T>, name: String? = null) {
        message(id, msgClass, name)
        codec(msgClass, serializer)
    }

    fun <T : Any> codec(domainClazz: KClass<T>, serializer: KSerializer<T>)

    fun <T : Any> fixCodec(msgClass: KClass<T>, codecAction: FixMessageCodecSpec<T>.() -> Unit)
}