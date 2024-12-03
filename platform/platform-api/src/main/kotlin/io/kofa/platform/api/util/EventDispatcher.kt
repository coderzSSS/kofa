package io.kofa.platform.api.util;

interface EventDispatcher<T: Any> {
    fun isInterested(eventType: Int): Boolean

    fun dispatch(eventType: Int, ctx: EventContext, event: T)
}
