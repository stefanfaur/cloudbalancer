plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "cloudbalancer"

include("common")
include("dispatcher")
include("worker")
include("metrics-aggregator")
