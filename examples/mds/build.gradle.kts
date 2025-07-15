plugins {
    alias(libs.plugins.ksp)
    kotlin("kapt")
    application
}

afterEvaluate {
    ksp {
        arg("kofa.classpath", sourceSets.main.get().runtimeClasspath.asPath)
        arg("kofa.rootDir", project.projectDir.absolutePath)
        arg("kofa.domain.master", "src/main/resources/mds-master.xml")
    }
}


application {
    mainClass = "io.kofa.platform.launcher.LauncherMainKt"
}

dependencies {
    implementation(rootProject.libs.kotlinx.datetime)
    implementation(rootProject.libs.kotlin.dataframe)

    implementation(rootProject.libs.agrona)
    implementation(rootProject.libs.sbe)
    compileOnly(rootProject.libs.autoServiceAnnotation)
    annotationProcessor(rootProject.libs.autoService)
    kapt(rootProject.libs.autoService)
    ksp(project(":platform:platform-codegen"))
}