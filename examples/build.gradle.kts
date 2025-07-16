subprojects {
    group = "io.kofa.example"

    apply(plugin = "org.jetbrains.kotlin.jvm")

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        dependsOn(project(":platform:platform-core").tasks.named("build"))
    }

    dependencies {
        "implementation"(project(":platform:platform-api"))
        "runtimeOnly"(project(":platform:platform-launcher"))
    }
}