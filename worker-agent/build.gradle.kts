plugins {
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":common"))
    implementation(libs.spring.boot.starter)
    implementation(libs.spring.boot.starter.kafka)
    implementation(libs.docker.java.core)
    implementation(libs.docker.java.transport)
    implementation(libs.resilience4j.spring.boot3)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.mockito.core)
}
