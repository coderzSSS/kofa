package io.kofa.platform.api.dsl.builder

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.kofa.platform.api.dsl.ComponentSpec
import io.kofa.platform.api.dsl.model.ComponentDefinition
import io.kofa.platform.api.inject.ComponentModuleDeclaration
import io.kofa.platform.api.inject.InjectContext
import io.kofa.platform.api.util.EventContext
import io.kofa.platform.api.util.EventDispatcher
import kotlin.reflect.KClass

class ComponentSpecBuilder<T : Any>(val injectContextFactory: () -> InjectContext) : ComponentSpec<T> {
    private var description: String? = null

    private val modules: MutableList<ComponentModuleDeclaration> = mutableListOf()
    private val dispatchers: MutableSet<EventDispatcher<out T>> = mutableSetOf()

    private val eventHandlers: MutableMap<KClass<out T>, EventContext.(T) -> Unit> = mutableMapOf()
    private var startAction: Option<() -> Unit> = None
    private var stopAction: Option<() -> Unit> = None

    private var errorHandler: Option<(Throwable) -> Unit> = None

    override val injectContext: InjectContext get() = injectContextFactory()

    override fun install(vararg modules: ComponentModuleDeclaration) {
        this.modules.addAll(modules)
    }

    override fun <E : T> withEventDispatcher(eventDispatcher: EventDispatcher<E>) {
        this.dispatchers.add(eventDispatcher)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <E : T> onEvent(eventClazz: KClass<E>, handler: EventContext.(E) -> Unit) {
        this.eventHandlers.putIfAbsent(eventClazz, handler as EventContext.(T) -> Unit)
    }

    override fun onStart(action: () -> Unit) {
        startAction = Some(action)
    }

    override fun onStop(action: () -> Unit) {
        stopAction = Some(action)
    }

    override fun onError(action: (Throwable) -> Unit) {
        errorHandler = Some(action)
    }

    internal fun build(id: String): Either<String, ComponentDefinition<T>> {
        return either {
            ensure(eventHandlers.isNotEmpty()) { "no event handler specified." }
            ComponentDefinition(
                id,
                description,
                modules,
                dispatchers.toList(),
                eventHandlers,
                stopAction,
                stopAction,
                errorHandler
            )
        }
    }
}