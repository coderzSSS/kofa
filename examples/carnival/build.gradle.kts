plugins {
    kotlin("kapt")
    application
}

application {
    mainClass = "io.kofa.platform.launcher.LauncherMainKt"
}

sourceSets {
    sourceSets.main {
        java.srcDir("build/generated/java")
    }
}

dependencies {
    implementation(rootProject.libs.kotlinx.datetime)

    implementation(rootProject.libs.agrona)
    implementation(rootProject.libs.sbe)
    compileOnly(rootProject.libs.autoServiceAnnotation)
    annotationProcessor(rootProject.libs.autoService)
    kapt(rootProject.libs.autoService)
}

task(name = "generateSbeMessages", type = JavaExec::class) {
    group = "sbe"
    mainClass = "uk.co.real_logic.sbe.SbeTool"
    classpath = sourceSets.main.get().compileClasspath
    systemProperties(
        "sbe.output.dir" to "build/generated/java",
        "sbe.target.language" to "Java",
        "sbe.validation.stop.on.error" to "true",
        "sbe.xinclude.aware" to "true",
        "sbe.validation.xsd" to "src/main/resources/sbe/sbe.xsd"
    )
    args = listOf("src/main/resources/domain/domain-sbe.xml")
}