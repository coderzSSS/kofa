package io.kofa.platform.core.internal.component.impl

import arrow.core.None
import arrow.core.Option
import io.kofa.platform.api.inject.ComponentModuleDeclaration
import io.kofa.platform.api.util.EventDispatcher
import io.kofa.platform.core.internal.component.ComponentConfig
import org.koin.core.Koin

internal class MessageHandlerComponent<E: Any>(
    koin: Koin,
    componentConfig: ComponentConfig,
    modules: List<ComponentModuleDeclaration>,
    private val handlers: List<EventDispatcher<E>>,
    private val startAction: Option<() -> Unit> = None,
    private val stopAction: Option<() -> Unit> = None,
    private val errorHandler: Option<(Throwable) -> Unit> = None
): ScopedComponent(componentConfig, modules, koin), EventDispatcher<E> {
    override fun isInterested(eventType: Int): Boolean {
        return handlers.any { it.isInterested(eventType) }
    }

    override fun dispatch(eventType: Int, event: E) {
        handlers.filter { it.isInterested(eventType) }.forEach {
            runCatching {
                it.dispatch(eventType, event)
            }.onFailure {
                e -> errorHandler.getOrNull()?.invoke(e)
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