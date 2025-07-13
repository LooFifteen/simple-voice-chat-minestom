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
        target(21)
        testWith(21)
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
    compileOnly(libs.minestom)
    testImplementation(libs.minestom)

    // error-prone
    errorprone(libs.errorprone)

    // testing
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.logback)
}

tasks.test {
    useJUnitPlatform()
}