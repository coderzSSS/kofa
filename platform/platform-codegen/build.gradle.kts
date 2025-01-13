plugins {
    id("com.github.bjornvester.xjc") version "1.8.2"
}

xjc {
    xsdDir.set(file("src/main/resources/xsd"))
    includes.addAll("domain.xsd", "types.xsd")
    markGenerated.set(true)
    defaultPackage.set("io.kofa.platform.codegen.xsd.generated")
    outputJavaDir = file("build/generated/java")
}

sourceSets {
    sourceSets.main {
        java.srcDir("build/generated/java")
        kotlin.srcDir("build/generated/kotlin")
    }
}

dependencies {
    implementation(rootProject.libs.sbe)
    implementation(rootProject.libs.kotinpoet)
    implementation(rootProject.libs.kotinpoet.ksp)

    compileOnly(rootProject.libs.ksp)

    implementation(project(":platform:platform-api"))

    testImplementation(rootProject.libs.kotlinCompileTestingKsp)

    implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.2")
    runtimeOnly("org.glassfish.jaxb:jaxb-runtime:4.0.5")
}
