package dev.lu15.voicechat.network.minecraft.packets.serverbound;

import dev.lu15.voicechat.network.minecraft.Packet;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

public record HandshakePacket(int version) implements Packet<HandshakePacket> {

    public static final @NotNull NetworkBuffer.Type<HandshakePacket> SERIALIZER = NetworkBufferTemplate.template(
            NetworkBuffer.INT, HandshakePacket::version,
            HandshakePacket::new
    );

    @Override
    public @NotNull String id() {
        return "voicechat:request_secret";
    }

    @Override
    public NetworkBuffer.@NotNull Type<HandshakePacket> serializer() {
        return SERIALIZER;
    }

}
