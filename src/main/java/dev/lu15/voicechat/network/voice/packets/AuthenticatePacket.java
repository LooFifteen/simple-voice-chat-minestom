package dev.lu15.voicechat.network.voice.packets;

import dev.lu15.voicechat.network.voice.VoicePacket;
import java.util.UUID;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

public record AuthenticatePacket(
        @NotNull UUID player,
        @NotNull UUID secret
) implements VoicePacket {

    public AuthenticatePacket(@NotNull NetworkBuffer buffer) {
        this(
                buffer.read(NetworkBuffer.UUID),
                buffer.read(NetworkBuffer.UUID)
        );
    }

    @Override
    public void write(@NotNull NetworkBuffer writer) {
        writer.write(NetworkBuffer.UUID, this.player);
        writer.write(NetworkBuffer.UUID, this.secret);
    }

    @Override
    public int id() {
        return 0x5;
    }

}
