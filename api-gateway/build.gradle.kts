dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.cloud:spring-cloud-starter-gateway")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation("org.springframework.security:spring-security-oauth2-jose")
    implementation("io.micrometer:micrometer-tracing-bridge-brave")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
