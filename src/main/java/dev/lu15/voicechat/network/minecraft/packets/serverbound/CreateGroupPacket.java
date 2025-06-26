package dev.lu15.voicechat.network.minecraft.packets.serverbound;

import dev.lu15.voicechat.group.Group;
import dev.lu15.voicechat.network.NetworkTypes;
import dev.lu15.voicechat.network.minecraft.Packet;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record CreateGroupPacket(
        @NotNull String name,
        @Nullable String password,
        @NotNull Group.Type type
) implements Packet<CreateGroupPacket> {

    public static final @NotNull NetworkBuffer.Type<CreateGroupPacket> SERIALIZER = NetworkBufferTemplate.template(
            NetworkBuffer.STRING, CreateGroupPacket::name,
            NetworkBuffer.STRING.optional(), CreateGroupPacket::password,
            NetworkTypes.ShortEnum(Group.Type.class), CreateGroupPacket::type,
            CreateGroupPacket::new
    );

    @Override
    public @NotNull String id() {
        return "voicechat:create_group";
    }

    @Override
    public NetworkBuffer.@NotNull Type<CreateGroupPacket> serializer() {
        return SERIALIZER;
    }

}
