plugins {
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.6"
    groovy
    application
}

group = "ru.softmotion"
version = "0.0.1-SNAPSHOT"
description = "XML Parser Service with Groovy"

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.postgresql:postgresql")
    implementation("org.apache.groovy:groovy")
    implementation("org.apache.groovy:groovy-xml")
    implementation("org.projectlombok:lombok")

    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.spockframework:spock-core:2.3-groovy-4.0") {
        exclude(group = "org.codehaus.groovy")
    }
    testImplementation("org.spockframework:spock-spring:2.3-groovy-4.0") {
        exclude(group = "org.codehaus.groovy")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

application {
    mainClass.set("ru.softmotion.xmlparser.XmlParserApplication")
}