package io.kofa.platform.api.logger

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging

typealias Logger = KLogger

val <reified T> T.logger: KLogger inline get() = KotlinLogging.logger {}
