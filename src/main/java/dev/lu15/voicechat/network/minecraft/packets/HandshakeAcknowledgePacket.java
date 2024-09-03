package dev.lu15.voicechat.network.minecraft.packets;

import dev.lu15.voicechat.Codec;
import dev.lu15.voicechat.network.minecraft.Packet;
import java.util.UUID;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

public record HandshakeAcknowledgePacket(
        @NotNull UUID secret,
        int port,
        @NotNull UUID player,
        @NotNull Codec codec,
        int mtu,
        double distance,
        int keepAlive,
        boolean groups,
        @NotNull String host,
        boolean recording
) implements Packet {

    private static final @NotNull NetworkBuffer.Type<Codec> CODEC_NETWORK_TYPE = Packet.ByteEnum(Codec.class);

    public HandshakeAcknowledgePacket(@NotNull NetworkBuffer buffer) {
        this(
                buffer.read(NetworkBuffer.UUID),
                buffer.read(NetworkBuffer.INT),
                buffer.read(NetworkBuffer.UUID),
                buffer.read(CODEC_NETWORK_TYPE),
                buffer.read(NetworkBuffer.INT),
                buffer.read(NetworkBuffer.DOUBLE),
                buffer.read(NetworkBuffer.INT),
                buffer.read(NetworkBuffer.BOOLEAN),
                buffer.read(NetworkBuffer.STRING),
                buffer.read(NetworkBuffer.BOOLEAN)
        );
    }

    @Override
    public void write(@NotNull NetworkBuffer writer) {
        writer.write(NetworkBuffer.UUID, this.secret);
        writer.write(NetworkBuffer.INT, this.port);
        writer.write(NetworkBuffer.UUID, this.player);
        writer.write(CODEC_NETWORK_TYPE, this.codec);
        writer.write(NetworkBuffer.INT, this.mtu);
        writer.write(NetworkBuffer.DOUBLE, this.distance);
        writer.write(NetworkBuffer.INT, this.keepAlive);
        writer.write(NetworkBuffer.BOOLEAN, this.groups);
        writer.write(NetworkBuffer.STRING, this.host);
        writer.write(NetworkBuffer.BOOLEAN, this.recording);
    }

    @Override
    public @NotNull String id() {
        return "voicechat:secret";
    }

}
