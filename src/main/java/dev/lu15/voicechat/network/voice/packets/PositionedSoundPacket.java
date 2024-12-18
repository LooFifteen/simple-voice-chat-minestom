package dev.lu15.voicechat.network.voice.packets;

import dev.lu15.voicechat.network.NetworkTypes;
import dev.lu15.voicechat.network.voice.Flags;
import dev.lu15.voicechat.network.voice.VoicePacket;
import java.util.Optional;
import java.util.UUID;
import net.minestom.server.coordinate.Point;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record PositionedSoundPacket(
        @NotNull UUID channel,
        @NotNull UUID sender,
        @NotNull Point position,
        byte @NotNull[] data,
        long sequenceNumber,
        float distance,
        @Nullable String category
) implements VoicePacket<PositionedSoundPacket> {

    public static final @NotNull NetworkBuffer.Type<PositionedSoundPacket> SERIALIZER = NetworkBufferTemplate.template(
            NetworkBuffer.UUID, PositionedSoundPacket::channel,
            NetworkBuffer.UUID, PositionedSoundPacket::sender,
            NetworkTypes.POSITION, PositionedSoundPacket::position,
            NetworkBuffer.BYTE_ARRAY, PositionedSoundPacket::data,
            NetworkBuffer.LONG, PositionedSoundPacket::sequenceNumber,
            NetworkBuffer.FLOAT, PositionedSoundPacket::distance,
            Flags.SERIALIZER, packet -> Flags.category(packet.category),
            PositionedSoundPacket::new
    );

    private PositionedSoundPacket(
            @NotNull UUID channel,
            @NotNull UUID sender,
            @NotNull Point position,
            byte @NotNull[] data,
            long sequenceNumber,
            float distance,
            @NotNull Flags flags
    ) {
        this(
                channel,
                sender,
                position,
                data,
                sequenceNumber,
                distance,
                flags.category()
        );
    }

    @Override
    public int id() {
        return 0x4;
    }

    @Override
    public NetworkBuffer.@NotNull Type<PositionedSoundPacket> serializer() {
        return SERIALIZER;
    }

}
