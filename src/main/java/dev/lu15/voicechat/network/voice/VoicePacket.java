package dev.lu15.voicechat.network.voice;

import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

public interface VoicePacket<T extends VoicePacket<T>> {

    int id();

    @NotNull NetworkBuffer.Type<T> serializer();

    default long ttl() {
        return 10_000;
    }

}
