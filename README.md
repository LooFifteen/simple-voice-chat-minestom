# Simple Voice Chat for Minestom

> [!CAUTION]
> This library is in development. It is not feature-complete but is usable. Please report any issues you find.

### Features
- [x] Proximity voice chat
- [x] Sound categories
- [ ] Groups
- [ ] Customisation
  - [ ] Codec
  - [x] Distance
  - [ ] MTU
  - [ ] Keepalive interval
  - [ ] Recording

```kts
repositories {
    mavenCentral() // minestom
    maven("https://repo.hypera.dev/snapshots/") // simple-voice-chat-minestom
}

dependencies {
    implementation("dev.lu15:simple-voice-chat-minestom:0.1.0-SNAPSHOT")
}
```

```java
// create a new voice chat server, you can use the same port as the Minecraft bind
VoiceChat voiceChat = VoiceChat.builder("0.0.0.0", 25565).enable();
```