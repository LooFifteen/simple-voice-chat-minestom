plugins {
    `java-library`
    signing
    id("net.kyori.indra")
    id("net.kyori.indra.publishing")
    id("net.ltgt.errorprone") version "4.0.1"
}

group = "dev.lu15"
version = "0.1.0-SNAPSHOT"

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
    val minestom = "net.minestom:minestom-snapshots:b1ad94cd1b"
    compileOnly(minestom)
    testImplementation(minestom)

    // error-prone
    errorprone("com.google.errorprone:error_prone_core:2.31.0")

    // testing
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("ch.qos.logback:logback-classic:1.5.7")
}

tasks.test {
    useJUnitPlatform()
}