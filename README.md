# Simple Voice Chat for Minestom

```kts
repositories {
    mavenCentral()
    mavenLocal() // this library currently needs local publishing
}

dependencies {
    implementation("dev.lu15:simple-voice-chat-minestom:0.1.0-SNAPSHOT")
}
```

```java
// create a new voice chat server, you can use the same port as the Minecraft bind
VoiceChat voiceChat = VoiceChat.builder("0.0.0.0", 25565).enable();
```