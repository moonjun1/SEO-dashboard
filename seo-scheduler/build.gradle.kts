plugins {
    id("org.springframework.boot") version "3.3.7" apply false
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":seo-common"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")

    runtimeOnly("org.postgresql:postgresql")
}

tasks.getByName<Jar>("jar") {
    enabled = true
}
