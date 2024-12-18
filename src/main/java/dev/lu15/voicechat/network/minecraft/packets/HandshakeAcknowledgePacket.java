package dev.lu15.voicechat.network.minecraft.packets;

import dev.lu15.voicechat.Codec;
import dev.lu15.voicechat.network.minecraft.Packet;
import java.util.UUID;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
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
) implements Packet<HandshakeAcknowledgePacket> {

    public static final @NotNull NetworkBuffer.Type<HandshakeAcknowledgePacket> SERIALIZER = NetworkBufferTemplate.template(
            NetworkBuffer.UUID, HandshakeAcknowledgePacket::secret,
            NetworkBuffer.INT, HandshakeAcknowledgePacket::port,
            NetworkBuffer.UUID, HandshakeAcknowledgePacket::player,
            Packet.ByteEnum(Codec.class), HandshakeAcknowledgePacket::codec,
            NetworkBuffer.INT, HandshakeAcknowledgePacket::mtu,
            NetworkBuffer.DOUBLE, HandshakeAcknowledgePacket::distance,
            NetworkBuffer.INT, HandshakeAcknowledgePacket::keepAlive,
            NetworkBuffer.BOOLEAN, HandshakeAcknowledgePacket::groups,
            NetworkBuffer.STRING, HandshakeAcknowledgePacket::host,
            NetworkBuffer.BOOLEAN, HandshakeAcknowledgePacket::recording,
            HandshakeAcknowledgePacket::new
    );

    @Override
    public @NotNull String id() {
        return "voicechat:secret";
    }

    @Override
    public NetworkBuffer.@NotNull Type<HandshakeAcknowledgePacket> serializer() {
        return SERIALIZER;
    }

}
