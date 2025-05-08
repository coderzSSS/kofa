package io.kofa.platform.api.annotation

import io.kofa.platform.api.util.NoopMessageBusService
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class DomainModule(val componentType: String, val handlerClass: KClass<*> = NoopMessageBusService::class)
