package io.kofa.platform.core.internal.controller

import io.kofa.platform.api.dsl.DomainDeclaration
import io.kofa.platform.api.logger.logger
import io.kofa.platform.api.meta.MessageMetaProvider
import io.kofa.platform.api.util.EventDispatcher
import io.kofa.platform.core.internal.component.Component
import io.kofa.platform.core.internal.component.ComponentLoader
import io.kofa.platform.core.internal.component.config.ComponentConfig
import io.kofa.platform.core.internal.service.EventBusService
import io.kofa.platform.core.internal.service.meta.MessageMetaRegistry
import io.kofa.platform.core.internal.thread.eventloop.EventLoop
import io.kofa.platform.core.internal.thread.eventloop.NamedTask
import io.kofa.platform.core.internal.thread.policy.ShutdownPriority
import org.koin.core.Koin
import org.koin.dsl.module
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException

internal class ApplicationController(private val koin: Koin) {
    private val eventLoop: EventLoop = koin.get()
    private val components = mutableListOf<Component>()

    fun configure(components: Collection<ComponentConfig>) {
        val componentMap = components.groupBy {
            it.type
        }

        val duplicates = componentMap.mapValues {
            it.value - it.value.distinct().toSet()
        }.filterValues {
            it.isNotEmpty()
        }

        duplicates.forEach {
            check(it.value.isEmpty()) {
                "component[${it.key}] found duplicate instance - ${it.value}"
            }
        }

        componentMap.forEach {
            val notAllowMultiInstance = it.value.any { config ->
                !config.allowMultipleInstance
            }

            if (notAllowMultiInstance) {
                check(it.value.size == 1) {
                    "multiple instance not allowed for component ${it.key}"
                }
            }
        }

        // load message meta
        ServiceLoader.load(DomainDeclaration::class.java).forEach { domainDeclaration ->
            val metaDefinition = domainDeclaration.getDomainDeclaration()
            MessageMetaRegistry.loadFrom(metaDefinition)

            val globalModule = module {
                metaDefinition.modules.forEach { moduleDeclaration -> moduleDeclaration.invoke(this) }
            }

            koin.loadModules(listOf(globalModule))
        }

        MessageMetaRegistry.addProviders(koin.getAll<MessageMetaProvider>())

        // load component meta
        val componentLoader = ComponentLoader(koin, koin.getAll<EventDispatcher>())
        componentLoader.loadComponents(components)

        this.components.addAll(componentLoader.getLoadedComponents())
    }

    private fun initEventBusService(): EventBusService {
        val eventBusService = koin.get<EventBusService>()
        if (eventBusService is Component) {
            initComponent(eventBusService)
        }

        return eventBusService
    }

    @Suppress("UNCHECKED_CAST")
    fun start() {
        val busService = initEventBusService()

        // init components, platform component will be initialized first
        this.components.forEach {
            initComponent(it)

            if (it is EventDispatcher) {
                busService.addDispatcher(it)
            }
        }

        eventLoop.onShutdownExecute("close application koin injector", ShutdownPriority.ResourceCleanup) {
            koin.close()
        }

        // start main event loop
        eventLoop.start()
    }

    // should not be called explicitly, normally is triggered by kill signal
    fun stop() {
        // stop event loop
        eventLoop.stop()
        runCatching {
            eventLoop.join()
            logger.info { "event loop stopped" }
        }.onFailure {
            when (it) {
                is TimeoutException -> {
                    logger.warn(it) { "Timed out waiting for event loop to shutdown" }
                }

                is ExecutionException -> {
                    logger.warn(it) { "EventLoop thread terminated on exception" }
                }
            }
        }
    }

    private fun initComponent(component: Component) {
        component.initialize()
        if (component is NamedTask) {
            eventLoop.addTask(component)
        }

        eventLoop.onStartExecute("Component[${component.id()}]-Start") {
            component.start()
        }

        eventLoop.onShutdownExecute("Component[${component.id()}]-Stop") {
            component.stop()
        }
    }
}
