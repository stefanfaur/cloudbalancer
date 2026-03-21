plugins {
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":common"))
    implementation(libs.spring.boot.starter)
    implementation(libs.spring.boot.starter.kafka)
    implementation(libs.resilience4j.spring.boot3)
    implementation(libs.resilience4j.micrometer)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.testcontainers.core)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.kafka)
}
