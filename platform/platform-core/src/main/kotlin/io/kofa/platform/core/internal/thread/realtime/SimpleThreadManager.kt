package io.kofa.platform.core.internal.thread.realtime

import java.util.concurrent.ThreadFactory

class SimpleThreadManager : ThreadManager {
    override fun enabled(): Boolean {
        return false
    }

    override fun applyConfiguredRealtimeFeatures(threadRole: String?) {
        // noop
    }

    override fun applyDefaultCpuAffinity() {
        // noop
    }

    override fun newThreadFactory(threadRole: String?, daemon: Boolean): ThreadFactory {
        return ThreadFactory {
            val thread = Thread(it, threadRole)
            thread.setDaemon(daemon)

            thread
        }
    }
}
