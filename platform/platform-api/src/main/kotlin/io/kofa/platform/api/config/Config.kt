package io.kofa.platform.api.config

import java.math.BigInteger
import kotlin.reflect.KClass
import kotlin.time.Duration

interface Config {
    fun <T: Any> getConfigMap(path: String, kClass: KClass<T>): Map<String, T>

    fun getConfigMap(path: String): Map<String, Config>

    fun getConfig(path: String): Config

    fun hasPath(path: String): Boolean

    fun getString(path: String): String

    fun getBoolean(path: String): Boolean

    fun getInt(path: String): Int

    fun getLong(path: String): Long

    fun getDouble(path: String): Double

    fun getDuration(path: String): Duration

    /**
     * @return size in bytes
     */
    fun getMemorySize(path: String): BigInteger

    fun <T : Any> extract(clazz: KClass<T>, path: String? = null): T
}

inline fun <reified T : Any> Config.getOrNull(path: String): T? {
    if (!hasPath(path)) {
        return null;
    }

    return when (T::class) {
        Boolean::class -> getBoolean(path) as T
        String::class -> getString(path) as T
        Int::class -> getInt(path) as T
        Long::class -> getLong(path) as T
        Double::class -> getDouble(path) as T
        Duration::class -> getDuration(path) as T
        BigInteger::class -> getMemorySize(path) as T
        else -> extract(T::class, path)
    }
}

inline fun <reified T : Any> Config.extract(path: String? = null): T {
    return extract(T::class, path)
}

inline fun <reified T : Any> Config.extractOrNull(path: String) = getOrNull<T>(path)

inline fun <reified T : Any> Config.getOrDefault(path: String, value: T) = getOrNull<T>(path) ?: value

