import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    base
    alias(libs.plugins.versions)
    alias(libs.plugins.versionCatalogUpdate)
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

allprojects {
    repositories {
        maven { url = uri("https://repo.huaweicloud.com/repository/maven") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        mavenCentral()
    }
}

versionCatalogUpdate {

}

val kotlinProjects = subprojects.filter { project ->
    val srcDir = project.file("src/main/kotlin")
    srcDir.exists() && srcDir.isDirectory && !srcDir.list().isNullOrEmpty()
}

configure(kotlinProjects) {
    apply {
        plugin("org.jetbrains.kotlin.jvm")
        plugin("org.jetbrains.kotlin.plugin.serialization")
        plugin(rootProject.libs.plugins.dokka.get().pluginId)
    }

    group = "io.kofa"
    version = rootProject.version

    dependencies {
//        "implementation"(enforcedPlatform(rootProject.libs.kotlin.bom))
        "implementation"(rootProject.libs.kotlinx.datetime)
        "implementation"(rootProject.libs.kotlinx.serialization.json)
        "implementation"(rootProject.libs.kotlinx.coroutine)
        "implementation"(kotlin("stdlib"))
        "implementation"(kotlin("reflect"))

        "testImplementation"(kotlin("test"))
        "testImplementation"(rootProject.libs.bundles.backend.test) {
            exclude("org.jetbrains.kotlin")
            exclude("org.jetbrains.kotlinx")
        }

        "testRuntimeOnly"(rootProject.libs.bundles.backend.test.runtime) {
            exclude("org.jetbrains.kotlin")
            exclude("org.jetbrains.kotlinx")
        }
    }

    tasks {
        withType<KotlinJvmCompile>().configureEach {
            compilerOptions {
                freeCompilerArgs = listOf("-Xcontext-receivers")
            }
        }

        named<Test>("test") {
            testLogging {
                events("passed", "skipped", "failed")
                showStandardStreams = true
            }

            useJUnitPlatform()
        }
    }

    configure<KotlinBaseExtension> {
        jvmToolchain(17)
    }
}

subprojects {
    afterEvaluate {
        extensions.findByType(PublishingExtension::class)?.run {
            logger.info("configure publishing extension for ${project.path}")
            repositories {
                mavenLocal()
            }

            publications {
                create<MavenPublication>("maven") {
                    from(components["java"])
                }
            }
        }
    }
}

tasks {
    withType<DokkaMultiModuleTask>().configureEach {
        val outputFile = file("docs")

        doFirst {
            delete(outputFile)
        }

        outputDirectory.set(outputFile)
        moduleName.set("kofa")
    }
}