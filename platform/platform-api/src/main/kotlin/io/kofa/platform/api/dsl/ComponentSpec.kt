package io.kofa.platform.api.dsl

import io.kofa.platform.api.annotation.KofaDsl
import io.kofa.platform.api.inject.ComponentModuleDeclaration
import io.kofa.platform.api.inject.InjectContext
import io.kofa.platform.api.util.EventContext
import io.kofa.platform.api.util.EventDispatcher
import kotlin.reflect.KClass

@KofaDsl
interface ComponentSpec<T: Any> {
    val injectContext: InjectContext

    fun install(vararg modules: ComponentModuleDeclaration)

    fun <E: T> withEventDispatcher(eventDispatcher: EventDispatcher)

    fun <E: T> onEvent(eventClazz: KClass<E>, handler: suspend EventContext.(E) -> Unit)

    fun onStart(action: suspend () -> Unit)

    fun onStop(action: suspend () -> Unit)

    fun onError(action: (Throwable) -> Unit)
}

inline fun <reified V: Any> ComponentSpec<*>.inject(name: String? = null) = lazy {
    injectContext.get(V::class, name)
}