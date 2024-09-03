pluginManagement {
    plugins {
        val indra = "3.1.3"
        id("net.kyori.indra") version indra
        id("net.kyori.indra.publishing") version indra
        id("net.kyori.indra.publishing.sonatype") version indra
    }
}

rootProject.name = "simple-voice-chat-minestom"

