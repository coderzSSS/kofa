package io.kofa.platform.api.util

fun interface EventHandler<T> {
    fun onEvent(event: T)
}