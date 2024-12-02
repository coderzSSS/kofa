package io.kofa.platform.core.internal.thread.eventloop

internal data class InvocationContext(
    var casualEventSequenceId: Long = 0,
    var processingTimestampNanos: Long = 0
) {
    fun reset() {
        casualEventSequenceId = 0
        processingTimestampNanos = System.nanoTime()
    }
}
