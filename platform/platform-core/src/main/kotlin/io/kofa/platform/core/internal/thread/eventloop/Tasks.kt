package io.kofa.platform.core.internal.thread.eventloop

internal sealed interface NamedTask : AutoCloseable {
    val name: String get() = toString()

    /**
     * daemon task won't stop application from stopping
     */
    val daemon: Boolean get() = true

    override fun close() {}
}

internal abstract class IterableTask(override val name: String) : NamedTask {
    override val daemon: Boolean get() = false

    /**
     * trigger invocation, return true if it could be polled again
     */
    abstract suspend fun poll(context: InvocationContext): Boolean
}

internal abstract class OneOffTask(override val name: String) : NamedTask {
    abstract suspend fun run(context: InvocationContext?)
}

