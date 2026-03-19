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
        testRuntimeOnly(rootProject.libs.junit.platform.launcher)
        testImplementation(rootProject.libs.assertj.core)
    }

    tasks.withType<Test> {
        useJUnitPlatform()

        // Podman on macOS: containers run in a Linux VM where the socket path
        // differs from the macOS host path. Tell Testcontainers the VM-internal
        // socket so Ryuk can mount it correctly.
        if (System.getenv("TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE") == null) {
            val dockerHost = System.getenv("DOCKER_HOST") ?: ""
            if (dockerHost.contains("podman")) {
                environment("TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE", "/run/podman/podman.sock")
            }
        }
    }
}
