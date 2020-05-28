plugins {
    id("java")
}

group = "org.mysun"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(group = "com.fasterxml.jackson.core", name = "jackson-databind", version = "2.11.0")
    implementation("io.aeron:aeron-all:1.28.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.3.1")
}


tasks.test {
    useJUnitPlatform()
}