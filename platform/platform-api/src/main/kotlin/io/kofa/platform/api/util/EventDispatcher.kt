package io.kofa.platform.api.util;

interface EventDispatcher<T: Any> {
    fun isInterested(eventType: Int): Boolean

    fun dispatch(eventType: Int, event: T)
}
