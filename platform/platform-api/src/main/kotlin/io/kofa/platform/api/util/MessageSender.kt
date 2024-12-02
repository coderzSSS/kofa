package io.kofa.platform.api.util

interface MessageSender<T> {
    suspend fun send(message: T);
}