package io.kofa.platform.core.internal.thread.eventloop.config

import io.kofa.platform.api.util.TaskUtil
import io.kofa.platform.core.internal.thread.eventloop.EventLoop
import io.kofa.platform.core.internal.thread.eventloop.InvocationContext
import io.kofa.platform.core.internal.thread.eventloop.impl.DefaultEventLoop
import io.kofa.platform.core.internal.thread.eventloop.impl.TaskUtilImpl
import io.kofa.platform.core.internal.thread.realtime.SimpleThreadManager
import io.kofa.platform.core.internal.thread.realtime.ThreadManager
import org.koin.dsl.bind
import org.koin.dsl.module

internal fun eventLoopModule(config: EventLoopConfig) = module {
    single {
        config
    }

    single {
        InvocationContext()
    }

    single {
        SimpleThreadManager()
    }.bind(ThreadManager::class)

    single {
        DefaultEventLoop(config, get(), get())
    }.bind(EventLoop::class)

    single {
        TaskUtilImpl(get())
    }.bind(TaskUtil::class)
}