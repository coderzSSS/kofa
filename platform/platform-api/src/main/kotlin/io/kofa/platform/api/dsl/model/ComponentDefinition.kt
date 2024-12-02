package io.kofa.platform.api.dsl.model

import arrow.core.None
import arrow.core.Option
import io.kofa.platform.api.inject.ComponentModuleDeclaration
import io.kofa.platform.api.util.EventContext
import io.kofa.platform.api.util.EventDispatcher
import kotlin.reflect.KClass

data class ComponentDefinition<E: Any>(
    val name: String,
    val description: String?,
    val modules: List<ComponentModuleDeclaration>,
    val eventDispatchers: List<EventDispatcher<out E>>,
    val eventHandlers: Map<KClass<out E>, EventContext.(E) -> Unit>,
    val startAction: Option<() -> Unit> = None,
    val stopAction: Option<() -> Unit> = None,
    val errorHandler: Option<(Throwable) -> Unit> = None
)