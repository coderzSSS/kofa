plugins {
    id("com.github.bjornvester.xjc") version "1.8.2"
    idea
    kotlin("kapt")
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
        java.srcDir("build/generated/ksp/main/java")
    }

    sourceSets.test {
        java.srcDir("build/generated/ksp/test/java")
    }
}

idea {
    module {
        generatedSourceDirs.add(file("build/generated/ksp/test/java"))
    }
}

dependencies {
    implementation(rootProject.libs.sbe)
    implementation(rootProject.libs.kotinpoet)
    implementation(rootProject.libs.kotinpoet.ksp)

    implementation(rootProject.libs.ksp)

    implementation(project(":platform:platform-api"))

    implementation(rootProject.libs.autoServiceAnnotation)
    annotationProcessor(rootProject.libs.autoService)
    kapt(rootProject.libs.autoService)

    testImplementation(rootProject.libs.kotlinCompileTestingKsp)

    implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.2")
    runtimeOnly(rootProject.libs.jaxb)
}
