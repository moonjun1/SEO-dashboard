plugins {
    id("org.springframework.boot") version "3.3.7"
    id("io.spring.dependency-management")
}

val jjwtVersion: String by project
val springdocVersion: String by project

dependencies {
    implementation(project(":seo-common"))
    implementation(project(":seo-crawler"))
    implementation(project(":seo-scheduler"))
    implementation(project(":seo-ai"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation("org.jsoup:jsoup:1.18.3")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")

    implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.security:spring-security-test")
}
