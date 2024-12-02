package io.kofa.platform.core.internal.config
import com.typesafe.config.ConfigObject
import io.github.config4k.ClassContainer
import io.github.config4k.TypeReference
import io.github.config4k.readers.SelectReader
import io.kofa.platform.api.config.Config
import java.math.BigInteger
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.toKotlinDuration

internal class TypeSafeConfig(private val delegate: com.typesafe.config.Config) : Config {
    override fun getDouble(path: String): Double {
        return delegate.getDouble(path)
    }

    override fun getDuration(path: String): Duration {
        return delegate.getDuration(path).toKotlinDuration()
    }

    override fun getMemorySize(path: String): BigInteger {
        return delegate.getMemorySize(path).toBytesBigInteger()
    }

    override fun getConfig(path: String): Config {
        return TypeSafeConfig(delegate.getConfig(path))
    }

    override fun getConfigMap(path: String): Map<String, Config> {
        return buildMap {
            delegate.getObject(path).forEach {
                when (val v = it.value) {
                    is ConfigObject -> {
                        put(it.key, TypeSafeConfig(v.toConfig().resolveWith(delegate)))
                    }
                }
            }
        }
    }

    override fun hasPath(path: String): Boolean {
        return delegate.hasPath(path)
    }

    override fun getString(path: String): String {
        return delegate.getString(path)
    }

    override fun getBoolean(path: String): Boolean {
        return delegate.getBoolean(path)
    }

    override fun getInt(path: String): Int {
        return delegate.getInt(path)
    }

    override fun getLong(path: String): Long {
        return delegate.getLong(path)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> extract(clazz: KClass<T>, path: String?): T {
        val genericType = object : TypeReference<T>() {}.genericType()

        val result = if (path != null) {
            SelectReader.getReader(ClassContainer(clazz, genericType))(delegate, path)
        } else {
            SelectReader.extractWithoutPath(
                ClassContainer(clazz, genericType),
                delegate
            )
        }

        return try {
            result as T
        } catch (e: Exception) {
            throw e
        }
    }
}
