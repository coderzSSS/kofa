package io.kofa.platform.api.annotation

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class DomainModule(val componentType: String, val handlerClass: KClass<*>)
