package dev.lu15.voicechat.network.voice.packets;

import dev.lu15.voicechat.network.voice.VoicePacket;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

public record KeepAlivePacket() implements VoicePacket<KeepAlivePacket> {

    public static final @NotNull NetworkBuffer.Type<KeepAlivePacket> SERIALIZER = NetworkBufferTemplate.template(
            KeepAlivePacket::new
    );

    @Override
    public int id() {
        return 0x8;
    }

    @Override
    public NetworkBuffer.@NotNull Type<KeepAlivePacket> serializer() {
        return SERIALIZER;
    }

}
