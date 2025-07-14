package io.kofa.platform.api.util

interface TaskUtil {
    fun scheduleRun(name: String, task: suspend () -> Unit)
}