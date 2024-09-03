# Simple Voice Chat for Minestom

> [!CAUTION]
> This library is in development and is not recommended for production use. It is missing critical features which could lead to memory leaks and other issues.

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