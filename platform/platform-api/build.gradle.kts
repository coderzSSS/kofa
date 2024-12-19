plugins {
    kotlin("jvm")
    id("maven-publish")
}

dependencies {
    api(rootProject.libs.koin)
    api(rootProject.libs.arrow.core)
    api(rootProject.libs.kotlin.logging)
    api(rootProject.libs.agrona)

    implementation(rootProject.libs.config4k)

    runtimeOnly(rootProject.libs.logback)
    runtimeOnly(rootProject.libs.slf4j.api)
}
