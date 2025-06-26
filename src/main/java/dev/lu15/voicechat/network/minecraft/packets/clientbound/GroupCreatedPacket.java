package dev.lu15.voicechat.network.minecraft.packets.clientbound;

import dev.lu15.voicechat.group.Group;
import dev.lu15.voicechat.network.minecraft.Packet;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

public record GroupCreatedPacket(@NotNull Group group) implements Packet<GroupCreatedPacket> {

    public static final @NotNull NetworkBuffer.Type<GroupCreatedPacket> SERIALIZER = NetworkBufferTemplate.template(
            Group.NETWORK_TYPE, GroupCreatedPacket::group,
            GroupCreatedPacket::new
    );

    @Override
    public @NotNull String id() {
        return "voicechat:add_group";
    }

    @Override
    public NetworkBuffer.@NotNull Type<GroupCreatedPacket> serializer() {
        return SERIALIZER;
    }

}
