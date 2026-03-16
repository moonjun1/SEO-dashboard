plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "seo-dashboard"

include("seo-common")
include("seo-api")
include("seo-crawler")
include("seo-scheduler")
include("seo-ai")
