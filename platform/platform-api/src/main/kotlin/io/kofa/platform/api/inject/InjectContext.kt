package io.kofa.platform.api.inject

import kotlin.reflect.KClass

interface InjectContext {
    fun <T : Any> getOrNull(clazz: KClass<T>, name: String? = null): T?

    fun <T : Any> get(clazz: KClass<T>, name: String? = null): T {
        return getOrNull(clazz, name) ?: throw RuntimeException("injection not found $clazz, $name")
    }
}

inline fun <reified T : Any> InjectContext.get(name: String? = null) = get(T::class, name)

inline fun <reified T : Any> InjectContext.getOrNull(name: String? = null) = getOrNull(T::class, name)

inline fun <reified T : Any> InjectContext.inject(name: String? = null) = lazy {
    get(T::class, name)
}
