package dev.lu15.voicechat.network.minecraft.packets.serverbound;

import dev.lu15.voicechat.network.minecraft.Packet;
import java.util.UUID;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record JoinGroupPacket(
        @NotNull UUID group,
        @Nullable String password
) implements Packet<JoinGroupPacket> {

    public static final @NotNull NetworkBuffer.Type<JoinGroupPacket> SERIALIZER = NetworkBufferTemplate.template(
            NetworkBuffer.UUID, JoinGroupPacket::group,
            NetworkBuffer.STRING.optional(), JoinGroupPacket::password,
            JoinGroupPacket::new
    );

    @Override
    public @NotNull String id() {
        return "voicechat:set_group";
    }

    @Override
    public NetworkBuffer.@NotNull Type<JoinGroupPacket> serializer() {
        return SERIALIZER;
    }

}
