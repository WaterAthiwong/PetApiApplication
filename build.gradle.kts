import org.jetbrains.kotlin.gradle.tasks.KotlinCompile // For `KotlinCompile` task below

plugins {
    id("org.springframework.boot") version "2.7.1"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.8.0" // The version of Kotlin to use
    kotlin("plugin.spring") version "1.8.0" // The Kotlin Spring plugin
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.0"
    id("com.bmuschko.docker-spring-boot-application") version "8.0.0"
}

group = "com.champaca"
version = "0.0.1"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

val exposedVersion = "0.47.0"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine:2.9.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin") // Jackson extensions for Kotlin for working with JSON
    implementation("org.jetbrains.kotlin:kotlin-reflect") // Kotlin reflection library, required for working with Spring
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8") // Kotlin standard library
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation("mysql:mysql-connector-java:8.0.11")
    implementation("io.github.evanrupert:excelkt:1.0.2")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-json:$exposedVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.7.1")
    implementation("ch.qos.logback:logback-classic:1.2.3") // Logback classic
    implementation("org.slf4j:slf4j-api:1.7.30") // SLF4J API
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.apache.commons:commons-text:1.10.0")
    implementation("com.lowagie:itext:2.1.7")
    implementation("com.github.librepdf:openpdf:1.3.30")
    implementation("net.sf.jasperreports:jasperreports:6.17.0")
    implementation("net.sf.jasperreports:jasperreports-fonts:6.17.0")
    implementation("net.sf.jasperreports:jasperreports-functions:6.17.0")
    implementation("io.github.g0dkar:qrcode-kotlin:4.0.6")
    implementation("com.google.zxing:core:3.4.1")
    implementation("com.google.zxing:javase:3.4.1")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions { // Kotlin compiler options
        freeCompilerArgs = listOf("-Xjsr305=strict") // `-Xjsr305=strict` enables the strict mode for JSR-305 annotations
        jvmTarget = "17" // This option specifies the target version of the generated JVM bytecode
    }
}

docker {
    springBootApplication {
        baseImage.set("amazoncorretto:17-alpine3.16")
        ports.set(listOf(8080))
    }
}

tasks.named<com.bmuschko.gradle.docker.tasks.image.Dockerfile>("dockerCreateDockerfile") {
    instruction("RUN mkdir reports")
    instruction("RUN apk update &&  apk add fontconfig freetype")
    instruction("RUN apk add --update ttf-dejavu && rm -rf /var/cache/apk/*")
    instruction("RUN apk add --update curl fontconfig && rm -rf /var/cache/apk/*")
    instruction("RUN mkdir -p /usr/share/fonts/TTF && cp /app/resources/reports/fonts/*.ttf /usr/share/fonts/TTF")
    instruction("RUN fc-cache -f -v")

    // Add Champaca cert into Java's keytool. The cert file locates at src/main/resources in codebase and resources/ in Docker
    instruction("RUN keytool -import -trustcacerts -file resources/champacaCert.crt -alias champaca -keystore \$JAVA_HOME/lib/security/cacerts -storepass changeit -noprompt")
}