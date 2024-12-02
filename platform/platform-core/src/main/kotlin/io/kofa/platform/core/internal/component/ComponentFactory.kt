package io.kofa.platform.core.internal.component

internal interface ComponentFactory {
    fun create(componentId: String): Component
}