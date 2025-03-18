# Simple Voice Chat for Minestom

> [!CAUTION]
> This library is in development. It is not feature-complete but is usable. Please report any issues you find.

### Features
- [x] Proximity voice chat
- [x] Sound categories
- [x] Groups
- [ ] Customisation
  - [ ] Codec
  - [x] Distance
  - [x] MTU
  - [ ] Keepalive interval
  - [ ] Recording

### Installation
<details>
<summary>Gradle (Kotlin)</summary>

```kts
repositories {
    mavenCentral()
    maven("https://https://jitpack.io")
}

dependencies {
    implementation("com.github.Summiner:simple-vc-minestom:main-SNAPSHOT")
}
```
</details>

<details>
<summary>Gradle (Groovy)</summary>

```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.Summiner:simple-vc-minestom:main-SNAPSHOT'
}
```
</details>

<details>
<summary>Maven</summary>

```xml
<repository>
  <id>jitpack.io</id>
  <url>https://jitpack.io</url>
</repository>

<dependency>
  <groupId>com.github.Summiner</groupId>
    <artifactId>simple-vc-minestom</artifactId>
  <version>Tag</version>
</dependency>
```
</details>

### Usage

<details>
<summary>Java Example</summary>

```java
VoiceChat voicechat = VoiceChat.builder("0.0.0.0", 21000) // create a new voice chat instance
        .setMTU(1024) // Set the mtu of the voice server. This is used determine the largest size of a packet.
        .publicAddress("voice.example.org:30000") // Set the public address of the voice server. This is used to tell clients where to connect to.
        .enable(); // Start the server
```
</details>

<details>
<summary>Kotlin Example (TODO)</summary>

```kotlin
// Under Construction
```
</details>



