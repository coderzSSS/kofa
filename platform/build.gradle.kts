subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    group = "io.kofa.platform"

    dependencies {
        "implementation"(rootProject.libs.arrow.core)
    }
}