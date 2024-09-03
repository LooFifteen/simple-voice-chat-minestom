package dev.lu15.voicechat.network.minecraft.packets;

import dev.lu15.voicechat.network.minecraft.Packet;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

public record HandshakePacket(int version) implements Packet {

    public HandshakePacket(@NotNull NetworkBuffer buffer) {
        this(buffer.read(NetworkBuffer.INT));
    }

    @Override
    public void write(@NotNull NetworkBuffer writer) {
        writer.write(NetworkBuffer.INT, this.version);
    }

    @Override
    public @NotNull String id() {
        return "voicechat:request_secret";
    }

}
