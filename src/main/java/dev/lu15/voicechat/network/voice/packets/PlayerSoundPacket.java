package dev.lu15.voicechat.network.voice.packets;

import dev.lu15.voicechat.network.voice.Flags;
import dev.lu15.voicechat.network.voice.VoicePacket;
import java.util.UUID;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record PlayerSoundPacket(
        @NotNull UUID channel,
        @NotNull UUID sender,
        byte @NotNull[] data,
        long sequenceNumber,
        float distance,
        boolean whispering,
        @Nullable String category
) implements VoicePacket {

    private PlayerSoundPacket(
            @NotNull NetworkBuffer buffer,
            @NotNull UUID channel,
            @NotNull UUID sender,
            byte @NotNull[] data,
            long sequenceNumber,
            float distance,
            byte flags
    ) {
        this(
                channel,
                sender,
                data,
                sequenceNumber,
                distance,
                (flags & Flags.WHISPERING) != 0,
                (flags & Flags.CATEGORY) != 0 ? buffer.read(NetworkBuffer.STRING) : null
        );
    }

    public PlayerSoundPacket(@NotNull NetworkBuffer buffer) {
        this(
                buffer,
                buffer.read(NetworkBuffer.UUID),
                buffer.read(NetworkBuffer.UUID),
                buffer.read(NetworkBuffer.BYTE_ARRAY),
                buffer.read(NetworkBuffer.LONG),
                buffer.read(NetworkBuffer.FLOAT),
                buffer.read(NetworkBuffer.BYTE)
        );
    }

    @Override
    public void write(@NotNull NetworkBuffer writer) {
        writer.write(NetworkBuffer.UUID, this.channel);
        writer.write(NetworkBuffer.UUID, this.sender);
        writer.write(NetworkBuffer.BYTE_ARRAY, this.data);
        writer.write(NetworkBuffer.LONG, this.sequenceNumber);
        writer.write(NetworkBuffer.FLOAT, this.distance);

        byte flags = 0;
        if (this.whispering) flags |= Flags.WHISPERING;
        if (this.category != null) flags |= Flags.CATEGORY;

        writer.write(NetworkBuffer.BYTE, flags);
        if (this.category != null) writer.write(NetworkBuffer.STRING, this.category);
    }

    @Override
    public int id() {
        return 0x2;
    }

}
