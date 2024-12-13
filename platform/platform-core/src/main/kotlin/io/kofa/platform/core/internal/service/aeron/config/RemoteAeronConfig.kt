package io.kofa.platform.core.internal.service.aeron.config

import java.net.URI

data class RemoteAeronConfig(
    val aeronConfig: AeronConfig,
    val url: URI // upstream url
)