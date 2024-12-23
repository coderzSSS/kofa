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
    private val dispatchers: MutableSet<Lazy<EventDispatcher>> = mutableSetOf()

    private val eventHandlers: MutableMap<KClass<out T>, suspend EventContext.(T) -> Unit> = mutableMapOf()
    private var startAction: Option<suspend () -> Unit> = None
    private var stopAction: Option<suspend () -> Unit> = None

    private var errorHandler: Option<(Throwable) -> Unit> = None

    override val injectContext: InjectContext by lazy { injectContextFactory() }

    override fun install(vararg modules: ComponentModuleDeclaration) {
        this.modules.addAll(modules)
    }

    override fun withEventDispatcher(eventDispatcher: Lazy<EventDispatcher>) {
        this.dispatchers.add(eventDispatcher)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <E : T> onEvent(eventClazz: KClass<E>, handler: suspend EventContext.(E) -> Unit) {
        this.eventHandlers.putIfAbsent(eventClazz, handler as suspend EventContext.(T) -> Unit)
    }

    override fun onStart(action: suspend () -> Unit) {
        startAction = Some(action)
    }

    override fun onStop(action: suspend () -> Unit) {
        stopAction = Some(action)
    }

    override fun onError(action: (Throwable) -> Unit) {
        errorHandler = Some(action)
    }

    internal fun build(id: String, type: String): Either<String, ComponentDefinition<T>> {
        return either {
            ensure(eventHandlers.isNotEmpty() || dispatchers.isNotEmpty()) { "no event handler specified." }
            ComponentDefinition(
                id = id,
                type = type,
                description = description,
                modules = modules,
                eventDispatchers = dispatchers.toList(),
                eventHandlers = eventHandlers,
                startAction = startAction,
                stopAction = stopAction,
                errorHandler = errorHandler
            )
        }
    }
}