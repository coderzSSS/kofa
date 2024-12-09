package io.kofa.platform.core.launcher

import com.typesafe.config.ConfigFactory
import io.kofa.platform.api.config.Config
import io.kofa.platform.core.internal.config.TypeSafeConfig
import java.io.File

object ConfigLoader {
    fun fromFile(files: Collection<String>): Config {
        var tempConfig: com.typesafe.config.Config? = null
        for (name in files) {
            val f = File(name)
            val c = if (f.exists() && f.isFile) {
                ConfigFactory.parseFile(f)
            } else {
                ConfigFactory.parseResources(name)
            }

            tempConfig?.withFallback(c)
            tempConfig = c
        }

        val defaultConfig = ConfigFactory.load()
        val config = tempConfig?.withFallback(defaultConfig) ?: defaultConfig

        config.resolve()
        return TypeSafeConfig(config)
    }

    fun fromString(str: String): Config {
        return TypeSafeConfig(ConfigFactory.parseString(str))
    }
}