package io.kofa.platform.core.internal.component

import io.kofa.platform.core.internal.component.config.ComponentConfig

internal interface ComponentFactory {
    fun create(componentConfig: ComponentConfig): Component
}