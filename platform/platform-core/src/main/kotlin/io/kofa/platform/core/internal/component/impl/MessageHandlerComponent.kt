package io.kofa.platform.core.internal.component.impl

import arrow.core.None
import arrow.core.Option
import io.kofa.platform.api.inject.ComponentModuleDeclaration
import io.kofa.platform.api.util.EventContext
import io.kofa.platform.api.util.EventDispatcher
import io.kofa.platform.core.internal.component.config.ComponentConfig
import org.koin.core.Koin
import kotlin.reflect.KClass

internal class MessageHandlerComponent(
    koin: Koin,
    componentConfig: ComponentConfig,
    modules: List<ComponentModuleDeclaration>,
    private val handlers: List<EventDispatcher>,
    private val startAction: Option<suspend () -> Unit> = None,
    private val stopAction: Option<suspend () -> Unit> = None,
    private val errorHandler: Option<(Throwable) -> Unit> = None
) : ScopedComponent(componentConfig, modules, koin), EventDispatcher {
    override fun isInterested(eventType: KClass<*>): Boolean {
        return handlers.any { it.isInterested(eventType) }
    }

    override suspend fun <T> dispatch(ctx: EventContext, event: T) {
        handlers.filter { event != null && it.isInterested(event::class) }.forEach {
            runCatching {
                it.dispatch(ctx, event)
            }.onFailure { e ->
                errorHandler.getOrNull()?.invoke(e)
            }
        }
    }

    override suspend fun start() {
        super.start()
        startAction.getOrNull()?.invoke()
    }

    override suspend fun stop() {
        stopAction.getOrNull()?.invoke()
        super.stop()
    }
}