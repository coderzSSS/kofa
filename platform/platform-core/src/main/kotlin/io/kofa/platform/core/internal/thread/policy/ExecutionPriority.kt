package io.kofa.platform.core.internal.thread.policy

internal sealed interface ExecutionPriority {
    val priority: Int
}

enum class ShutdownPriority(override val priority: Int) : ExecutionPriority {
    Default(10),

    ResourceCleanup(9999)
}

enum class PullPriority(override val priority: Int, private val batchEnabled: Boolean) : ExecutionPriority {
    Default(20, false),
    Admin(5, true),
    SystemAction(6, true),
    UserAction(10, false),
    Read(100, false);

    fun batchSize(): Int {
        return if (batchEnabled) {
            Int.MAX_VALUE
        } else {
            1
        }
    }
}

