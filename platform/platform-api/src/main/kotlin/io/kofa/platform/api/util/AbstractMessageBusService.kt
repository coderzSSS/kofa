package io.kofa.platform.api.util

import io.kofa.platform.api.config.Config
import io.kofa.platform.api.inject.InjectContext
import io.kofa.platform.api.inject.inject
import io.kofa.platform.api.logger.Logger
import kotlin.reflect.KClass

abstract class AbstractMessageBusService<T: Any>: EventDispatcher, InjectContext {
    private lateinit var injectContext: InjectContext

    fun setInjectContext(injectContext: InjectContext) {
        this.injectContext = injectContext
    }

    val messageSender: MessageSender<T> by inject()
    val logger: Logger by inject()
    val config: Config by inject()

    protected suspend fun sendMessage(msg: T) {
        messageSender.send(msg)
    }

    open suspend fun onStartup() {}

    open suspend fun onShutdown() {}

    open suspend fun onException(e: Throwable) {
        throw e
    }

    override fun <T : Any> getOrNull(clazz: KClass<T>, name: String?): T? {
        return injectContext.get<T>(clazz, name)
    }
}