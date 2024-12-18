package dev.lu15.voicechat.network.minecraft.packets.serverbound;

import dev.lu15.voicechat.network.minecraft.Packet;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

public record LeaveGroupPacket() implements Packet<LeaveGroupPacket> {

    public static final @NotNull NetworkBuffer.Type<LeaveGroupPacket> SERIALIZER = NetworkBufferTemplate.template(
            LeaveGroupPacket::new
    );

    @Override
    public @NotNull String id() {
        return "voicechat:leave_group";
    }

    @Override
    public NetworkBuffer.@NotNull Type<LeaveGroupPacket> serializer() {
        return SERIALIZER;
    }

}
