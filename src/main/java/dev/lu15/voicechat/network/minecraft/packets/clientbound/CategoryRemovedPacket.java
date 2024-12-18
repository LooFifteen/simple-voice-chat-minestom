package dev.lu15.voicechat.network.minecraft.packets.clientbound;

import dev.lu15.voicechat.network.minecraft.Packet;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

public record CategoryRemovedPacket(@NotNull String category) implements Packet<CategoryRemovedPacket> {

    public static final @NotNull NetworkBuffer.Type<CategoryRemovedPacket> SERIALIZER = NetworkBufferTemplate.template(
            NetworkBuffer.STRING, CategoryRemovedPacket::category,
            CategoryRemovedPacket::new
    );

    @Override
    public @NotNull String id() {
        return "voicechat:remove_category";
    }

    @Override
    public NetworkBuffer.@NotNull Type<CategoryRemovedPacket> serializer() {
        return SERIALIZER;
    }

}
