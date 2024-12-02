package io.kofa.platform.api.annotation

@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class Domain(val pkgName: String = "", val domainName: String = "")
