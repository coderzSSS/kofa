pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        gradlePluginPortal()
    }
}


plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

rootProject.name = "kofa"
include("examples")
include("examples:carnival")
include("examples:mds")

include("platform")
include("platform:platform-api")
include("platform:platform-core")
include("platform:platform-launcher")
include("platform:platform-codegen")

