package dev.lu15.voicechat.network.minecraft.packets.clientbound;

import dev.lu15.voicechat.network.minecraft.Packet;
import net.kyori.adventure.key.Key;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

public record CategoryRemovedPacket(@NotNull String category) implements Packet<CategoryRemovedPacket> {

    public static final @NotNull NetworkBuffer.Type<CategoryRemovedPacket> SERIALIZER = NetworkBufferTemplate.template(
            NetworkBuffer.STRING, CategoryRemovedPacket::category,
            CategoryRemovedPacket::new
    );

    public CategoryRemovedPacket(@NotNull Key category) {
        this(category.asString().replace(':', '_'));
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
