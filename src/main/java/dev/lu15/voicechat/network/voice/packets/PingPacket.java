package dev.lu15.voicechat.network.voice.packets;

import dev.lu15.voicechat.network.voice.VoicePacket;
import java.util.UUID;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

public record PingPacket(
        @NotNull UUID player,
        long timestamp
) implements VoicePacket {

    public PingPacket(@NotNull NetworkBuffer buffer) {
        this(
                buffer.read(NetworkBuffer.UUID),
                buffer.read(NetworkBuffer.LONG)
        );
    }

    @Override
    public void write(@NotNull NetworkBuffer writer) {
        writer.write(NetworkBuffer.UUID, this.player);
        writer.write(NetworkBuffer.LONG, this.timestamp);
    }

    @Override
    public int id() {
        return 0x7;
    }

}
