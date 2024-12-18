package dev.lu15.voicechat.network.voice.packets;

import dev.lu15.voicechat.network.voice.VoicePacket;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

public record YeaImHerePacket() implements VoicePacket<YeaImHerePacket> {

    public static final @NotNull NetworkBuffer.Type<YeaImHerePacket> SERIALIZER = NetworkBufferTemplate.template(
            YeaImHerePacket::new
    );

    @Override
    public int id() {
        return 0xA;
    }

    @Override
    public NetworkBuffer.@NotNull Type<YeaImHerePacket> serializer() {
        return SERIALIZER;
    }

}
