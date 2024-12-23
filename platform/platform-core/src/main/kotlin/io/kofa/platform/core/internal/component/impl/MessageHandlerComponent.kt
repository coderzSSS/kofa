package io.kofa.platform.core.internal.component.impl

import arrow.core.None
import arrow.core.Option
import io.kofa.platform.api.inject.ComponentModuleDeclaration
import io.kofa.platform.api.logger.logger
import io.kofa.platform.api.util.AbstractMessageBusService
import io.kofa.platform.api.util.EventContext
import io.kofa.platform.api.util.EventDispatcher
import io.kofa.platform.core.internal.component.config.ComponentConfig
import org.koin.core.Koin
import kotlin.reflect.KClass

internal class MessageHandlerComponent(
    koin: Koin,
    componentConfig: ComponentConfig,
    modules: List<ComponentModuleDeclaration>,
    private val eagerDispatchers: List<EventDispatcher>,
    private val lazyDispatchers: List<Lazy<EventDispatcher>>,
    private val startAction: Option<suspend () -> Unit> = None,
    private val stopAction: Option<suspend () -> Unit> = None,
    private val errorHandler: Option<(Throwable) -> Unit> = None
) : ScopedComponent(componentConfig, modules, koin), EventDispatcher {
    private val dispatchers by lazy {
        eagerDispatchers + lazyDispatchers.map { d -> d.value }
    }

    private val handlers by lazy {
        val result = dispatchers.mapNotNull { dispatcher -> dispatcher as? AbstractMessageBusService<*> }
        result.forEach { handler ->
            handler.setInjectContext(this)
        }

        result
    }

    override fun isInterested(eventType: KClass<*>): Boolean {
        return dispatchers.any { it.isInterested(eventType) }
    }

    override suspend fun <T> dispatch(ctx: EventContext, event: T) {
        dispatchers.filter { event != null && it.isInterested(event::class) }.forEach {
            runCatching {
                it.dispatch(ctx, event)
            }.onFailure { e ->
                handlers.forEach { handler -> handler.onException(e) }
                errorHandler.getOrNull()?.invoke(e)
                throw e
            }
        }
    }

    override suspend fun start() {
        super.start()

        //DON"T DELETE init dispatchers
        logger.trace { "${dispatchers.size}" }

        handlers.forEach { handler -> handler.onStartup()}

        startAction.getOrNull()?.invoke()
    }

    override suspend fun stop() {
        handlers.forEach { handler -> handler.onShutdown() }

        stopAction.getOrNull()?.invoke()

        super.stop()
    }
}