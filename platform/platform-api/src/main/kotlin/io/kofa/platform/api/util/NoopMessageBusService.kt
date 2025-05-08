package io.kofa.platform.api.util

import kotlin.reflect.KClass

class NoopMessageBusService: AbstractMessageBusService<Any>() {
    override fun isInterested(eventType: KClass<*>): Boolean {
        return false
    }

    override suspend fun <T> dispatch(ctx: EventContext, event: T) {
    }
}