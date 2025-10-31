package io.kofa.platform.api.util

import io.kofa.platform.api.codec.DirectBufferCodec
import io.kofa.platform.api.config.Config
import io.kofa.platform.api.inject.InjectContext
import io.kofa.platform.api.inject.inject
import io.kofa.platform.api.logger.Logger
import io.kofa.platform.api.message.EventHeader
import io.kofa.platform.api.meta.MessageMetaProvider
import kotlin.reflect.KClass

abstract class AbstractMessageBusService<T : Any>(private val qualifier: String? = null) : EventDispatcher,
    LazyEventDispatcher, InjectContext {
    private lateinit var injectContext: InjectContext

    fun setInjectContext(injectContext: InjectContext) {
        this.injectContext = injectContext
    }

    val messageSender: MessageSender<T> by inject()
    val logger: Logger by inject()
    val config: Config by inject()

    val metaProvider: MessageMetaProvider by inject(qualifier)
    val directBufferCodec: DirectBufferCodec by inject(qualifier)

    private fun checkEventType(eventType: Int): Boolean {
        return metaProvider.tryGetMessageMeta(eventType) != null
    }

    override suspend fun <T> dispatch(
        ctx: EventContext,
        eventProvider: (DirectBufferCodec) -> Pair<EventHeader, T>
    ) {
        val result = eventProvider.invoke(directBufferCodec)
        if (checkEventType(result.first.eventType)) {
            dispatch(ctx, result.second)
        }
    }

    override fun isInterested(eventType: KClass<*>): Boolean = metaProvider.tryGetMessageMeta(eventType) != null

    protected suspend fun sendMessage(msg: T) {
        messageSender.send(msg)
    }

    fun initialize() {
        logger.info { "init message sender: $messageSender" }
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