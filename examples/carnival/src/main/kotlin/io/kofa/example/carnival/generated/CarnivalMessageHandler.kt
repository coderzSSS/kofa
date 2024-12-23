package io.kofa.example.carnival.generated

import io.kofa.example.carnival.domain.message.CarnivalEvent.Apple
import io.kofa.example.carnival.domain.message.CarnivalEvent.Banana
import io.kofa.platform.api.util.AbstractMessageBusService
import io.kofa.platform.api.util.EventContext
import kotlin.reflect.KClass

abstract class CarnivalMessageHandler : AbstractMessageBusService<Any>() {
    protected open suspend fun onBananaEvent(event: Banana) {}
    protected open suspend fun onAppleEvent(event: Apple) {}

    override fun isInterested(eventType: KClass<*>): Boolean {
        return CarnivalMessageConstants.tryGetMessageMeta(eventType) != null
    }

    override suspend fun <T> dispatch(ctx: EventContext, event: T) {
        when (event) {
            is Banana -> onBananaEvent(event)
            is Apple -> onAppleEvent(event)
        }
    }
}