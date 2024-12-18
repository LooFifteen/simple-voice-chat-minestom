package dev.lu15.voicechat.network.minecraft.packets.clientbound;

import dev.lu15.voicechat.network.minecraft.Category;
import dev.lu15.voicechat.network.minecraft.Packet;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

public record CategoryAddedPacket(@NotNull Category category) implements Packet<CategoryAddedPacket> {

    public static final @NotNull NetworkBuffer.Type<CategoryAddedPacket> SERIALIZER = NetworkBufferTemplate.template(
            Category.NETWORK_TYPE, CategoryAddedPacket::category,
            CategoryAddedPacket::new
    );

    @Override
    public @NotNull String id() {
        return "voicechat:add_category";
    }

    @Override
    public NetworkBuffer.@NotNull Type<CategoryAddedPacket> serializer() {
        return SERIALIZER;
    }

}
