package dev.lu15.voicechat.network.voice.packets;

import dev.lu15.voicechat.network.voice.VoicePacket;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

public record MicrophonePacket(
        byte @NotNull[] data,
        long sequenceNumber,
        boolean whispering
) implements VoicePacket<MicrophonePacket> {

    public static final @NotNull NetworkBuffer.Type<MicrophonePacket> SERIALIZER = NetworkBufferTemplate.template(
            NetworkBuffer.BYTE_ARRAY, MicrophonePacket::data,
            NetworkBuffer.LONG, MicrophonePacket::sequenceNumber,
            NetworkBuffer.BOOLEAN, MicrophonePacket::whispering,
            MicrophonePacket::new
    );

    @Override
    public int id() {
        return 0x1;
    }

    @Override
    public NetworkBuffer.@NotNull Type<MicrophonePacket> serializer() {
        return SERIALIZER;
    }

    @Override
    public long ttl() {
        return 500;
    }

}
