package io.kofa.platform.core.internal.config

import com.typesafe.config.ConfigFactory
import io.kofa.platform.api.config.Config
import java.io.File

object ConfigLoader {
    fun fromFile(vararg file: String): Config {
        var tempConfig: com.typesafe.config.Config? = null
        for (name in file) {
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
