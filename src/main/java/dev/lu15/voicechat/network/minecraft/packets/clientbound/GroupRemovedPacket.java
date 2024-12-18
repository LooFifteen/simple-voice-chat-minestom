package dev.lu15.voicechat.network.minecraft.packets.clientbound;

import dev.lu15.voicechat.network.minecraft.Packet;
import java.util.UUID;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

public record GroupRemovedPacket(@NotNull UUID group) implements Packet<GroupRemovedPacket> {

    public static final @NotNull NetworkBuffer.Type<GroupRemovedPacket> SERIALIZER = NetworkBufferTemplate.template(
            NetworkBuffer.UUID, GroupRemovedPacket::group,
            GroupRemovedPacket::new
    );

    @Override
    public @NotNull String id() {
        return "voicechat:remove_group";
    }

    @Override
    public NetworkBuffer.@NotNull Type<GroupRemovedPacket> serializer() {
        return SERIALIZER;
    }

}
