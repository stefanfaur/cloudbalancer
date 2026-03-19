plugins {
    java
}

allprojects {
    group = "com.cloudbalancer"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    dependencies {
        // Spring Boot BOM for version management (no Spring Boot runtime)
        implementation(platform(rootProject.libs.spring.boot.bom))
        testImplementation(platform(rootProject.libs.testcontainers.bom))
        testImplementation(rootProject.libs.junit.jupiter)
        testImplementation(rootProject.libs.assertj.core)
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
