plugins {
    kotlin("kapt")
}

dependencies {
    implementation(rootProject.libs.kotlinx.datetime)
    implementation(rootProject.libs.autoServiceAnnotation)

    annotationProcessor(rootProject.libs.autoService)
    kapt(rootProject.libs.autoService)
}