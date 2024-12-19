package dev.lu15.voicechat.network.minecraft.packets.clientbound;

import dev.lu15.voicechat.network.minecraft.Packet;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

public record CategoryRemovedPacket(@NotNull String category) implements Packet<CategoryRemovedPacket> {

    public static final @NotNull NetworkBuffer.Type<CategoryRemovedPacket> SERIALIZER = NetworkBufferTemplate.template(
            NetworkBuffer.STRING, CategoryRemovedPacket::category,
            CategoryRemovedPacket::new
    );

    public CategoryRemovedPacket(@NotNull NamespaceID category) {
        this(category.toString().replace(':', '_'));
    }

    public CategoryRemovedPacket {
        if (category.length() > 16) throw new IllegalArgumentException("category id is too long, found " + category.length() + " characters, maximum is 16");
    }

    @Override
    public @NotNull String id() {
        return "voicechat:remove_category";
    }

    @Override
    public NetworkBuffer.@NotNull Type<CategoryRemovedPacket> serializer() {
        return SERIALIZER;
    }

}
