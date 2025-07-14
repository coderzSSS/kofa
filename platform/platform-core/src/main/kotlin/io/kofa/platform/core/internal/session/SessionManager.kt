package io.kofa.platform.core.internal.session

import io.kofa.platform.api.config.getOrDefault
import io.kofa.platform.api.inject.inject
import io.kofa.platform.api.util.TaskUtil
import io.kofa.platform.core.internal.component.impl.PlatformMessageHandlerComponent
import io.kofa.platform.core.internal.launcher.PlatformConfig
import io.kofa.platform.message.SessionStartedMessage
import io.kofa.platform.message.SessionStoppedMessage
import org.koin.core.Koin

internal class SessionManager(platformConfig: PlatformConfig, koin: Koin) :
    PlatformMessageHandlerComponent("PlatformSessionManager", platformConfig.config.getConfig("session"), koin) {
    private val autoStart = config.getOrDefault<Boolean>("autoStart", true)

    private var sessionId: String? = null;

    private val taskUtil: TaskUtil by inject()

    override suspend fun start() {
        if (autoStart) {
            sessionId = createSessionId()
            logger.info { "Session started: $sessionId" }
            taskUtil.scheduleRun("send session started") {
                messageSender.send(SessionStartedMessage(sessionId))
            }
        }
    }

    private fun createSessionId(): String {
        return getDomain() + System.currentTimeMillis().toString()
    }

    override suspend fun stop() {
        if (sessionId != null) {
            messageSender.send(SessionStoppedMessage(sessionId))
        }
    }
}