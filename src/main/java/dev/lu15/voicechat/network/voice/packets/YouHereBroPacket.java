package dev.lu15.voicechat.network.voice.packets;

import dev.lu15.voicechat.network.voice.VoicePacket;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

public record YouHereBroPacket() implements VoicePacket<YouHereBroPacket> {

    public static final @NotNull NetworkBuffer.Type<YouHereBroPacket> SERIALIZER = NetworkBufferTemplate.template(
            YouHereBroPacket::new
    );

    @Override
    public int id() {
        return 0x9;
    }

    @Override
    public NetworkBuffer.@NotNull Type<YouHereBroPacket> serializer() {
        return SERIALIZER;
    }

}
