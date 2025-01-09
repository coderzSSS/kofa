package io.kofa.platform.codegen.writer.kotlin

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getKotlinClassByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import kotlin.reflect.KClass

object KspUtils {
    fun KSType.isAssignableFrom(kClass: KClass<*>, resolver: Resolver): Boolean {
        return kClass.asKsType(resolver)?.let { type ->
            this.isAssignableFrom(type)
        } == true
    }

    fun KSType.isAssignableFrom(kClass: ClassName, resolver: Resolver): Boolean {
        return kClass.asKsType(resolver)?.let { type ->
            this.isAssignableFrom(type)
        } == true
    }

    @OptIn(KspExperimental::class)
    fun KClass<*>.asKsType(resolver: Resolver) = qualifiedName?.let { name ->
        resolver.getKotlinClassByName(name)?.asStarProjectedType()
    }

    @OptIn(KspExperimental::class)
    fun ClassName.asKsType(resolver: Resolver) = resolver.getKotlinClassByName(canonicalName)?.asStarProjectedType()
}