package io.kofa.platform.core.internal.component.impl

import io.kofa.platform.api.inject.ComponentModuleDeclaration
import io.kofa.platform.api.inject.InjectContext
import io.kofa.platform.api.logger.Logger
import io.kofa.platform.core.internal.component.Component
import io.kofa.platform.core.internal.component.config.ComponentConfig
import io.kofa.platform.core.internal.component.config.logger
import io.kofa.platform.core.internal.component.config.source
import org.koin.core.Koin
import org.koin.core.component.KoinScopeComponent
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.bind
import org.koin.dsl.module
import kotlin.reflect.KClass

internal open class ScopedComponent(
    private val componentConfig: ComponentConfig,
    private val modules: List<ComponentModuleDeclaration>,
    private val originKoin: Koin
) : Component, KoinScopeComponent, InjectContext {
    private val scopedId = componentConfig.source

    override fun id(): String {
        return componentConfig.type
    }

    override fun initialize() {
        getKoin().loadModules(
            listOf(
                module {
                    scope(named(scopedId)) {
                        scoped {
                            componentConfig
                        }.bind()

                        val logger = componentConfig.logger

                        scoped {
                            logger
                        }.bind(Logger::class)

                        modules.forEach { m -> m.invoke(this) }
                    }
                }
            )
        )
    }

    override suspend fun start() {
    }

    override suspend fun stop() {
        if (scope.isNotClosed()) {
            scope.close()
        }
    }

    override fun getKoin(): Koin {
        return originKoin
    }

    override val scope: Scope by getOrCreateKoinScope()

    private fun getOrCreateKoinScope() = lazy {
        getKoin().getScopeOrNull(scopedId) ?: getKoin().createScope(scopedId, named(scopedId))
    }


    override fun <T : Any> getOrNull(clazz: KClass<T>, name: String?): T? {
        return if (name == null) {
            scope.getOrNull(clazz)
        } else {
            scope.getOrNull(clazz, named(name))
        }
    }

}