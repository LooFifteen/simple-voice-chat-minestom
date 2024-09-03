package dev.lu15.voicechat.network.voice.packets;

import dev.lu15.voicechat.network.voice.VoicePacket;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

public record MicrophonePacket(
        byte @NotNull[] data,
        long sequenceNumber,
        boolean whispering
) implements VoicePacket {

    public MicrophonePacket(@NotNull NetworkBuffer buffer) {
        this(
                buffer.read(NetworkBuffer.BYTE_ARRAY),
                buffer.read(NetworkBuffer.LONG),
                buffer.read(NetworkBuffer.BOOLEAN)
        );
    }

    @Override
    public void write(@NotNull NetworkBuffer writer) {
        writer.write(NetworkBuffer.BYTE_ARRAY, this.data);
        writer.write(NetworkBuffer.LONG, this.sequenceNumber);
        writer.write(NetworkBuffer.BOOLEAN, this.whispering);
    }

    @Override
    public int id() {
        return 0x1;
    }

    @Override
    public long ttl() {
        return 500;
    }

}
