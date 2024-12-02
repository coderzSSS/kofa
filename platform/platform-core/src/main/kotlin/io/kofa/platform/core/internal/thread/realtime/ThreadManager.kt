package io.kofa.platform.core.internal.thread.realtime

import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import kotlin.coroutines.CoroutineContext

internal interface ThreadManager {
    fun enabled(): Boolean

    fun applyConfiguredRealtimeFeatures(threadRole: String?)

    fun applyDefaultCpuAffinity()

    fun newThreadFactory(threadRole: String?, daemon: Boolean): ThreadFactory

    fun newThreadFactory(threadRole: String?): ThreadFactory {
        return newThreadFactory(threadRole, false)
    }

    fun coroutineContext(threadRole: String?, daemon: Boolean = false): CoroutineContext {
        val factory = newThreadFactory(threadRole, daemon)
        val executor = Executors.newSingleThreadExecutor(factory)
        return executor.asCoroutineDispatcher()
    }
}
