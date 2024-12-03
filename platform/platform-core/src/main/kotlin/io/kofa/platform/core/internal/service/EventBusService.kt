package io.kofa.platform.core.internal.service

import io.kofa.platform.api.util.EventDispatcher

internal interface EventBusService {
    fun addDispatcher(dispatcher: EventDispatcher<Any>)

    fun dispatch(eventType: Int, event: Any)
}