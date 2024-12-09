package io.kofa.platform.core.internal.thread.eventloop.impl

import com.google.common.base.Throwables
import com.google.common.collect.LinkedListMultimap
import com.google.common.collect.Multimap
import com.google.common.collect.Multimaps
import io.kofa.platform.api.logger.logger
import io.kofa.platform.core.internal.thread.eventloop.*
import io.kofa.platform.core.internal.thread.eventloop.EventLoop
import io.kofa.platform.core.internal.thread.eventloop.config.EventLoopConfig
import io.kofa.platform.core.internal.thread.policy.PullPriority
import io.kofa.platform.core.internal.thread.policy.ShutdownPriority
import io.kofa.platform.core.internal.thread.realtime.ThreadManager
import kotlinx.coroutines.*
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.DurationUnit
import kotlin.time.toDuration

internal class DefaultEventLoop(
    private val config: EventLoopConfig,
    private val invocationContext: InvocationContext,
    private val realtimeManager: ThreadManager
) : EventLoop {
    private val executionPolicy = config.executionPolicy
    private val startupTasks: Queue<OneOffTask> = ConcurrentLinkedQueue()
    private val shutdownTasks: Multimap<ShutdownPriority, OneOffTask> = Multimaps.synchronizedListMultimap(
        LinkedListMultimap.create()
    )
    private val pollingTasks: Multimap<PullPriority, NamedTask> =
        Multimaps.synchronizedListMultimap(LinkedListMultimap.create())

    @Volatile
    private var running: Boolean = false

    private val eventLoopScope: CoroutineScope =
        CoroutineScope(realtimeManager.coroutineContext(config.threadRole ?: "EventLoop"))

    @Volatile
    private var job: Job? = null

    @Volatile
    private var lastException: Throwable? = null

    override fun coroutineScope(): CoroutineScope {
        return eventLoopScope
    }

    private suspend fun execute() {
        try {
            logger.info { "Starting EventLoop" }

            runStartupTasks()
            logger.info { "startup tasks completed" }

            logger.info { "Entering Main Event Loop" }
            while (running) {
                loop()
            }
            logger.info { "Leaving Main Event Loop" }
            runShutdownTasks()
            logger.info { "Shutdown tasks completed" }
        } catch (e: Error) {
            lastException = e
            Throwables.throwIfUnchecked(e)
        } catch (e: RuntimeException) {
            lastException = e
            Throwables.throwIfUnchecked(e)
        } finally {
            logger.info { "Event loop exited" }
        }
    }

    private suspend fun runStartupTasks() {
        while (startupTasks.isNotEmpty()) {
            val r = startupTasks.poll()
            logger.info { "Running startup task: ${r.name}" }
            try {
                r.run(null)
            } catch (e: java.lang.Error) {
                running = false
                logger.error(e) { "Aborting startup: Failure when running task ${r.name}" }
                logger.info { "Triggering Shutdown tasks" }
                runShutdownTaskThenRethrow(e)
            } catch (e: java.lang.RuntimeException) {
                running = false
                logger.error(e) { "Aborting startup: Failure when running task ${r.name}" }
                logger.info { "Triggering Shutdown tasks" }
                runShutdownTaskThenRethrow(e)
            }
        }
    }

    private suspend fun loop() {
        iterateTaskContainer()
    }

    private suspend fun iterateTaskContainer(): Boolean {
        val shouldRun = AtomicBoolean(false)
        val hasWorkLeftAfterIteration = AtomicBoolean(false)

        pollingTasks.entries()
            .sortedBy {
                it.key.priority
            }.forEach { entry ->
                try {
                    val taskContainer = entry.value
                    if (!taskContainer.daemon) {
                        shouldRun.set(true)
                    }

                    when (taskContainer) {
                        is OneOffTask -> {
                            invocationContext.reset()
                            logger.trace { "executing task ${taskContainer.name}" }
                            taskContainer.run(invocationContext)
                        }

                        is IterableTask -> {
                            val batchSize: Int = entry.key.batchSize()
                            var hasMoreWork = true
                            for (i in 0 until batchSize) {
                                invocationContext.reset()
                                logger.trace { "polling from ${taskContainer.name} ${if (i > 0) " batch $i" else ""}" }
                                if (!taskContainer.poll(invocationContext)) {
                                    hasMoreWork = false
                                    break
                                }
                            }
                            hasWorkLeftAfterIteration.set(hasWorkLeftAfterIteration.get() || hasMoreWork)
                        }
                    }
                } catch (e: java.lang.RuntimeException) {
                    logger.error(e) { "Uncaught exception found in EventLoop" }
                    if (config.exitOnException) {
                        runShutdownTaskThenRethrow(e)
                    }
                }
            }

        pollingTasks.values().removeIf { task -> task is OneOffTask }

        if (!shouldRun.get() || pollingTasks.isEmpty) {
            running = false
        } else {
            executionPolicy.onEndOfEachCycle(hasWorkLeftAfterIteration.get())
        }
        return hasWorkLeftAfterIteration.get()
    }

    private suspend fun runShutdownTaskThenRethrow(e: Throwable) {
        try {
            runShutdownTasks()
        } catch (e1: java.lang.Error) {
            e.addSuppressed(e1)
        } catch (e1: java.lang.RuntimeException) {
            e.addSuppressed(e1)
        }
        Throwables.throwIfUnchecked(e)
    }

    private suspend fun runShutdownTasks() {
        val exception = AtomicReference<Throwable>()
        val e = closeTaskContainer()
        if (e != null) {
            exception.set(e)
        }
        shutdownTasks.entries().reversed()
            .sortedBy { it.key.priority }
            .forEach { entry ->
                val r = entry.value
                logger.info { "Running shutdown task: ${r.name}" }
                try {
                    r.run(null)
                } catch (e1: java.lang.Error) {
                    if (exception.get() == null) {
                        exception.set(e1)
                    } else {
                        exception.get()!!.addSuppressed(e1)
                    }
                    logger.error(e1) { "Failure detected when running shutdown task: ${r.name}" }
                } catch (e1: java.lang.RuntimeException) {
                    if (exception.get() == null) {
                        exception.set(e1)
                    } else {
                        exception.get()!!.addSuppressed(e1)
                    }
                    logger.error(e1) { "Failure detected when running shutdown task: ${r.name} " }
                }
            }
        if (exception.get() != null) {
            Throwables.throwIfUnchecked(exception.get())
        }
    }

    private fun closeTaskContainer(): Throwable? {
        var exception: Throwable? = null
        for (p in pollingTasks.values()) {
            try {
                p.close()
            } catch (e: java.lang.Error) {
                if (exception == null) {
                    exception = e
                } else {
                    exception.addSuppressed(e)
                }
            } catch (e: java.lang.RuntimeException) {
                if (exception == null) {
                    exception = e
                } else {
                    exception.addSuppressed(e)
                }
            }
        }
        return exception
    }

    override fun onStartExecute(name: String, task: suspend () -> Unit) {
        startupTasks.add(object : OneOffTask(name) {
            override suspend fun run(context: InvocationContext?) {
                task.invoke()
            }
        })
    }

    override fun onShutdownExecute(name: String, priority: ShutdownPriority, task: suspend () -> Unit) {
        shutdownTasks.put(
            priority,
            object : OneOffTask(name) {
                override suspend fun run(context: InvocationContext?) {
                    task.invoke()
                }
            }
        )
    }

    override fun addTask(task: NamedTask) {
        pollingTasks.put(PullPriority.Default, task)
    }

    override fun removeTask(taskName: String) {
        pollingTasks.values().removeIf {
            it.name == taskName
        }
    }

    override fun hasTask(taskName: String): Boolean {
        return pollingTasks.values().any {
            it.name == taskName
        }
    }

    override fun isRunning(): Boolean {
        return running
    }

    override fun start() {
        if (isRunning()) {
            return
        }

        running = true

        job = eventLoopScope.launch {
            execute()
        }
    }

    override fun stop() {
        if (isRunning()) {
            logger.info(RuntimeException("Marker exception to show trace")) { "Signal to stop event loop" }
            running = false
        }
    }

    override fun join() {
        if (job == null || !job!!.isActive) {
            return
        }

        val timeout = config.shutdownTimeoutSeconds.toDuration(DurationUnit.SECONDS)

        val startedTime = System.currentTimeMillis()
        if (timeout.isPositive()) {
            logger.info { "Waiting $timeout for application event loop to exit" }
            try {
                runBlocking {
                    withTimeout(timeout) {
                        job!!.cancelAndJoin()
                        eventLoopScope.cancel()
                    }
                }
            } catch (e: TimeoutCancellationException) {
                logger.warn(e) { "Interrupted waiting on EventLoop thread to exit" }
            }
        }

        val waitedFor = System.currentTimeMillis() - startedTime
        if (job!!.isActive) {
            throw TimeoutException("EventLoop Thread still alive after " + waitedFor + "ms")
        }

        if (lastException != null) {
            throw ExecutionException("EventLoop thread completed with exception", lastException)
        }
    }
}
