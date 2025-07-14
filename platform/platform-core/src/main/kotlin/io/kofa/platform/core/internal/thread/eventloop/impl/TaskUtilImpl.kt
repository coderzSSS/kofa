package io.kofa.platform.core.internal.thread.eventloop.impl

import io.kofa.platform.api.util.TaskUtil
import io.kofa.platform.core.internal.thread.eventloop.EventLoop
import io.kofa.platform.core.internal.thread.eventloop.InvocationContext
import io.kofa.platform.core.internal.thread.eventloop.OneOffTask

internal class TaskUtilImpl(private val eventLoop: EventLoop): TaskUtil {
    override fun scheduleRun(name: String, task: suspend () -> Unit) {
        eventLoop.addTask(object : OneOffTask(name) {
            override suspend fun run(context: InvocationContext?) {
                task.invoke()
            }
        })
    }
}