package dev.lu15.voicechat.network.minecraft.packets.serverbound;

import dev.lu15.voicechat.network.minecraft.Packet;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

public record UpdateStatePacket(boolean disabled) implements Packet<UpdateStatePacket> {

    public static final @NotNull NetworkBuffer.Type<UpdateStatePacket> SERIALIZER = NetworkBufferTemplate.template(
            NetworkBuffer.BOOLEAN, UpdateStatePacket::disabled,
            UpdateStatePacket::new
    );

    @Override
    public @NotNull String id() {
        return "voicechat:update_state";
    }

    @Override
    public NetworkBuffer.@NotNull Type<UpdateStatePacket> serializer() {
        return SERIALIZER;
    }

}
