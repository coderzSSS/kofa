package io.kofa.platform.core.internal.service

import io.kofa.platform.api.util.EventDispatcher

internal interface EventBusService<T: Any> {
    fun addDispatcher(dispatcher: EventDispatcher<T>)

    fun dispatch(event: T)
}