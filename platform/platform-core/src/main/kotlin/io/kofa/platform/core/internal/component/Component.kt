package io.kofa.platform.core.internal.component

import org.koin.core.Koin

/**
 * an organic component managed by the platform.
 *
 * user components are dynamically found and loaded in nature order
 *
 * platform components are statically managed
 */
internal interface Component {
    fun id(): String

    fun initialize()

    suspend fun start()

    suspend fun stop()
}

/**
 * a marker interface to indicate as a platform component which might be handled differently by the platform
 */
internal interface PlatformComponent : Component

internal abstract class AbstractComponentFactory() {

    internal abstract fun check(config: ComponentConfig): Boolean

    internal abstract fun create(config: ComponentConfig, koin: Koin): Component
}

