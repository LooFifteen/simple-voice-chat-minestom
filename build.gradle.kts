plugins {
    `java-library`
    signing
    id("net.kyori.indra")
    id("net.kyori.indra.publishing")
    id("net.ltgt.errorprone") version "4.3.0"
}

group = "dev.lu15"
version = "0.2.0-SNAPSHOT"

indra {
    mitLicense()
    github("LooFifteen", "simple-voice-chat-minestom") {
        ci(true)
    }

    signWithKeyFromPrefixedProperties("ci")
    publishSnapshotsTo("hyperaSnapshots", "https://repo.hypera.dev/snapshots/")

    javaVersions {
        target(25)
        testWith(25)
    }

    configurePublications {
        pom {
            inceptionYear = "2024"

            developers {
                developer {
                    id = "LooFifteen"
                    name = "Luis"
                    email = "luis@lu15.dev"
                    timezone = "Europe/London"
                }
            }
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // minestom
    val minestom = "net.minestom:minestom:2026.06.05-26.1.2"
    compileOnly(minestom)
    testImplementation(minestom)

    // logging
    compileOnly("org.slf4j:slf4j-api:2.0.17")

    // error-prone
    errorprone("com.google.errorprone:error_prone_core:2.50.0")

    // testing
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("ch.qos.logback:logback-classic:1.5.18")
}

tasks.test {
    useJUnitPlatform()
}