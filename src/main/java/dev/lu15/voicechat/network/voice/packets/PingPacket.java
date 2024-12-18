package dev.lu15.voicechat.network.voice.packets;

import dev.lu15.voicechat.network.voice.VoicePacket;
import java.util.UUID;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

public record PingPacket(
        @NotNull UUID player,
        long timestamp
) implements VoicePacket<PingPacket> {

    public static final @NotNull NetworkBuffer.Type<PingPacket> SERIALIZER = NetworkBufferTemplate.template(
            NetworkBuffer.UUID, PingPacket::player,
            NetworkBuffer.LONG, PingPacket::timestamp,
            PingPacket::new
    );

    @Override
    public int id() {
        return 0x7;
    }

    @Override
    public NetworkBuffer.@NotNull Type<PingPacket> serializer() {
        return SERIALIZER;
    }

}
