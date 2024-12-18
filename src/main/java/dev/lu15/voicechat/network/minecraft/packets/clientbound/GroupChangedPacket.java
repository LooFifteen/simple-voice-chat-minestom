package dev.lu15.voicechat.network.minecraft.packets.clientbound;

import dev.lu15.voicechat.network.minecraft.Packet;
import java.util.UUID;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record GroupChangedPacket(
        @Nullable UUID group,
        boolean incorrectPassword
) implements Packet<GroupChangedPacket> {

    public static final @NotNull NetworkBuffer.Type<GroupChangedPacket> SERIALIZER = NetworkBufferTemplate.template(
            NetworkBuffer.OPT_UUID, GroupChangedPacket::group,
            NetworkBuffer.BOOLEAN, GroupChangedPacket::incorrectPassword,
            GroupChangedPacket::new
    );

    @Override
    public @NotNull String id() {
        return "voicechat:joined_group";
    }

    @Override
    public NetworkBuffer.@NotNull Type<GroupChangedPacket> serializer() {
        return SERIALIZER;
    }

}
