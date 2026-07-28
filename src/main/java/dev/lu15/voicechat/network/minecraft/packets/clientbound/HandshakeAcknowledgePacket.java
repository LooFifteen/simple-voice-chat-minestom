package dev.lu15.voicechat.network.minecraft.packets.clientbound;

import dev.lu15.voicechat.Codec;
import dev.lu15.voicechat.VoiceChat;
import dev.lu15.voicechat.network.NetworkTypes;
import dev.lu15.voicechat.network.minecraft.Packet;
import dev.lu15.voicechat.network.voice.encryption.Secret;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

public record HandshakeAcknowledgePacket(
        @NotNull Secret secret,
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

    public static final @NotNull Key IDENTIFIER = VoiceChat.key("secret");
    public static final @NotNull NetworkBuffer.Type<HandshakeAcknowledgePacket> SERIALIZER = NetworkBufferTemplate.template(
            Secret.SERIALIZER, HandshakeAcknowledgePacket::secret,
            NetworkBuffer.INT, HandshakeAcknowledgePacket::port,
            NetworkBuffer.UUID, HandshakeAcknowledgePacket::player,
            NetworkTypes.ByteEnum(Codec.class), HandshakeAcknowledgePacket::codec,
            NetworkBuffer.INT, HandshakeAcknowledgePacket::mtu,
            NetworkBuffer.DOUBLE, HandshakeAcknowledgePacket::distance,
            NetworkBuffer.INT, HandshakeAcknowledgePacket::keepAlive,
            NetworkBuffer.BOOLEAN, HandshakeAcknowledgePacket::groups,
            NetworkBuffer.STRING, HandshakeAcknowledgePacket::host,
            NetworkBuffer.BOOLEAN, HandshakeAcknowledgePacket::recording,
            HandshakeAcknowledgePacket::new
    );

    @Override
    public @NotNull Key id() {
        return IDENTIFIER;
    }

    @Override
    public NetworkBuffer.@NotNull Type<HandshakeAcknowledgePacket> serializer() {
        return SERIALIZER;
    }

}
