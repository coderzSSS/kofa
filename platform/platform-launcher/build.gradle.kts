plugins {
    kotlin("kapt")
}

dependencies {
    implementation(project(":platform:platform-core"))
    implementation(rootProject.libs.kotlinx.coroutine)
    implementation(rootProject.libs.koin)
    implementation(rootProject.libs.config4k) {
        exclude("org.jetbrains.kotlin")
    }

    implementation(rootProject.libs.picocli)
    implementation(rootProject.libs.arrow.core) {
        exclude("org.jetbrains.kotlin")
    }
}
