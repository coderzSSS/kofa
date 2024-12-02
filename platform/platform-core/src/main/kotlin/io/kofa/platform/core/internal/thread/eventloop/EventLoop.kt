package io.kofa.platform.core.internal.thread.eventloop

import io.kofa.platform.core.internal.thread.policy.ShutdownPriority

/**
 * heart of the platform
 *
 * the startup, polling and shutdown sequence are managed by this component
 */
internal interface EventLoop {
    fun onStartExecute(name: String, task: suspend () -> Unit)

    fun onShutdownExecute(name: String, priority: ShutdownPriority = ShutdownPriority.Default, task: suspend () -> Unit)

    fun addTask(task: NamedTask)

    fun removeTask(taskName: String)

    fun hasTask(taskName: String): Boolean

    fun isRunning(): Boolean

    fun start()

    /**
     * stop the event loop, new events will not be processed anymore, in-flight events should not be impacted
     */
    fun stop()

    /**
     * wait for event loop thread to be done
     */
    fun join()
}

