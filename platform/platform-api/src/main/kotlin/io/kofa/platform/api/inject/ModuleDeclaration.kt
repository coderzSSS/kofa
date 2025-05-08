package io.kofa.platform.api.inject

import org.koin.dsl.ModuleDeclaration
import org.koin.dsl.ScopeDSL

typealias ComponentModuleDeclaration = ScopeDSL.() -> Unit

typealias ApplicationModuleDeclaration = ModuleDeclaration