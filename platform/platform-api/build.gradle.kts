plugins {
    id("maven-publish")
}

dependencies {
    api(rootProject.libs.koin)
    implementation(rootProject.libs.config4k)

    api(rootProject.libs.kotlin.logging)

    runtimeOnly(rootProject.libs.logback)
    runtimeOnly(rootProject.libs.slf4j.api)
}
