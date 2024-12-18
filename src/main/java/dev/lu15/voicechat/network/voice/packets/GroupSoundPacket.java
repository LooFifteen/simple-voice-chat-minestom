package dev.lu15.voicechat.network.voice.packets;

import dev.lu15.voicechat.network.voice.Flags;
import dev.lu15.voicechat.network.voice.VoicePacket;
import java.util.Optional;
import java.util.UUID;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record GroupSoundPacket(
        @NotNull UUID channel,
        @NotNull UUID sender,
        byte @NotNull[] data,
        long sequenceNumber,
        @Nullable String category
) implements VoicePacket<GroupSoundPacket> {

    public static final @NotNull NetworkBuffer.Type<GroupSoundPacket> SERIALIZER = NetworkBufferTemplate.template(
            NetworkBuffer.UUID, GroupSoundPacket::channel,
            NetworkBuffer.UUID, GroupSoundPacket::sender,
            NetworkBuffer.BYTE_ARRAY, GroupSoundPacket::data,
            NetworkBuffer.LONG, GroupSoundPacket::sequenceNumber,
            Flags.SERIALIZER, packet -> Flags.category(packet.category),
            GroupSoundPacket::new
    );

    private GroupSoundPacket(
            @NotNull UUID channel,
            @NotNull UUID sender,
            byte @NotNull[] data,
            long sequenceNumber,
            @NotNull Flags flags
    ) {
        this(
                channel,
                sender,
                data,
                sequenceNumber,
                flags.category()
        );
    }

    @Override
    public int id() {
        return 0x3;
    }

    @Override
    public NetworkBuffer.@NotNull Type<GroupSoundPacket> serializer() {
        return SERIALIZER;
    }

}
