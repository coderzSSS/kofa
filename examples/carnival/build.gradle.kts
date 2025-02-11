plugins {
    alias(libs.plugins.ksp)
    kotlin("kapt")
    idea
    application
}

ksp {
    arg("kofa.rootDir", project.projectDir.absolutePath)
    arg("kofa.domain.master", "src/main/resources/carnival-master.xml")
}

application {
    mainClass = "io.kofa.platform.launcher.LauncherMainKt"
}

sourceSets {
    sourceSets.main {
        java.srcDir("build/generated/java")
        kotlin.srcDir("build/generated/kotlin")
    }
}

idea {
    module {
        generatedSourceDirs.add(file("build/generated/java"))
        generatedSourceDirs.add(file("build/generated/kotlin"))
    }
}

dependencies {
    implementation(rootProject.libs.kotlinx.datetime)

    implementation(rootProject.libs.agrona)
    implementation(rootProject.libs.sbe)
    compileOnly(rootProject.libs.autoServiceAnnotation)
    annotationProcessor(rootProject.libs.autoService)
    kapt(rootProject.libs.autoService)
    ksp(project(":platform:platform-codegen"))
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
        "sbe.validation.xsd" to "../../platform/platform-codegen/src/main/resources/xsd/sbe.xsd"
    )
    args = listOf("src/main/resources/domain/domain-sbe.xml")
}

tasks {
    named("compileKotlin") {
        dependsOn("generateSbeMessages")
    }
}