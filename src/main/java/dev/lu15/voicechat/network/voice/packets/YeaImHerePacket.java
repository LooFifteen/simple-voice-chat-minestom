package dev.lu15.voicechat.network.voice.packets;

import dev.lu15.voicechat.network.voice.VoicePacket;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

public record YeaImHerePacket() implements VoicePacket {

    public YeaImHerePacket(@NotNull NetworkBuffer buffer) {
        this(); // no-op
    }

    @Override
    public void write(@NotNull NetworkBuffer writer) {
        // no-op
    }

    @Override
    public int id() {
        return 0xA;
    }

}