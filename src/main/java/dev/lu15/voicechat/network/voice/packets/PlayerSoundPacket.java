package dev.lu15.voicechat.network.voice.packets;

import dev.lu15.voicechat.network.voice.Flags;
import dev.lu15.voicechat.network.voice.VoicePacket;
import java.util.Optional;
import java.util.UUID;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
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
) implements VoicePacket<PlayerSoundPacket> {

    public static final @NotNull NetworkBuffer.Type<PlayerSoundPacket> SERIALIZER = NetworkBufferTemplate.template(
            NetworkBuffer.UUID, PlayerSoundPacket::channel,
            NetworkBuffer.UUID, PlayerSoundPacket::sender,
            NetworkBuffer.BYTE_ARRAY, PlayerSoundPacket::data,
            NetworkBuffer.LONG, PlayerSoundPacket::sequenceNumber,
            NetworkBuffer.FLOAT, PlayerSoundPacket::distance,
            Flags.SERIALIZER, packet -> Flags.flags(packet.whispering, packet.category),
            PlayerSoundPacket::new
    );

    private PlayerSoundPacket(
            @NotNull UUID channel,
            @NotNull UUID sender,
            byte @NotNull[] data,
            long sequenceNumber,
            float distance,
            @NotNull Flags flags
    ) {
        this(
                channel,
                sender,
                data,
                sequenceNumber,
                distance,
                flags.whispering(),
                flags.category()
        );
    }

    @Override
    public int id() {
        return 0x2;
    }

    @Override
    public NetworkBuffer.@NotNull Type<PlayerSoundPacket> serializer() {
        return SERIALIZER;
    }

}
