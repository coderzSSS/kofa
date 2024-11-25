plugins {
    kotlin("kapt")
}

dependencies {
    api(project(":platform:platform-api"))

    implementation(rootProject.libs.kotlinx.coroutine)
    implementation(rootProject.libs.koin)
    implementation(rootProject.libs.config4k) {
        exclude("org.jetbrains.kotlin")
    }

    implementation(rootProject.libs.arrow.core) {
        exclude("org.jetbrains.kotlin")
    }
    implementation(rootProject.libs.guava)
    implementation(rootProject.libs.chronicle.queue)
    implementation(rootProject.libs.fastutil)
    compileOnly(rootProject.libs.autoServiceAnnotation)
    annotationProcessor(rootProject.libs.autoService)
    kapt(rootProject.libs.autoService)
}
