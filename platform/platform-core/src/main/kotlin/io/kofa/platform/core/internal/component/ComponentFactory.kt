package io.kofa.platform.core.internal.component

internal interface ComponentFactory {
    fun create(componentConfig: ComponentConfig): Component
}