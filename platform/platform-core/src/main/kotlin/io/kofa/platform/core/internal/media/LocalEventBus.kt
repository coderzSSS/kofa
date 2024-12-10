package io.kofa.platform.core.internal.media

import io.kofa.platform.api.logger.logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.Executors

internal class LocalEventBus<T : Any> : AutoCloseable {
    private val flow = MutableSharedFlow<T>()

    private val events = flow.asSharedFlow()
    private val context = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(context)

    fun publish(event: T) {
        scope.launch {
            flow.emit(event)
        }
    }

    fun subscribe(handler: suspend (T) -> Unit) {
        events.onEach { event ->
            runCatching {
                handler(event)
            }.onFailure { error ->
                logger.error(error) { "exception found on subscribe" }
            }
        }.launchIn(this.scope)
    }

    override fun close() {
        scope.cancel()
    }

    suspend fun awaitClose() {
        if (scope.isActive) {
            if (scope.coroutineContext.isActive) {
                scope.coroutineContext[Job]?.cancelAndJoin()
            }
        }
    }
}