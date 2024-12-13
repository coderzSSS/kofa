plugins {
    kotlin("kapt")
    application
}

application {
    mainClass = "io.kofa.platform.launcher.LauncherMainKt"
}

dependencies {
    implementation(rootProject.libs.kotlinx.datetime)
    compileOnly(rootProject.libs.autoServiceAnnotation)

    annotationProcessor(rootProject.libs.autoService)
    kapt(rootProject.libs.autoService)
}