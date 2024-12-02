package io.kofa.platform.api.annotation

@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class DomainModule(val componentType: String)
