subprojects {
    group = "io.kofa.example"

    apply(plugin = "org.jetbrains.kotlin.jvm")
    
    dependencies {
        "implementation"(project(":platform:platform-api"))
        "runtimeOnly"(project(":platform:platform-launcher"))
    }
}