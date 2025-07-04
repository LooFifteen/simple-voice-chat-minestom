package dev.lu15.voicechat.network.minecraft.packets.clientbound;

import dev.lu15.voicechat.network.minecraft.Category;
import dev.lu15.voicechat.network.minecraft.Packet;
import java.util.regex.Pattern;

import net.kyori.adventure.key.Key;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

public record CategoryAddedPacket(@NotNull String identifier, @NotNull Category category) implements Packet<CategoryAddedPacket> {

    public static final @NotNull NetworkBuffer.Type<CategoryAddedPacket> SERIALIZER = NetworkBufferTemplate.template(
            NetworkBuffer.STRING, CategoryAddedPacket::identifier,
            Category.NETWORK_TYPE, CategoryAddedPacket::category,
            CategoryAddedPacket::new
    );
    public static final @NotNull Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-z_]{1,16}$");

    public CategoryAddedPacket(@NotNull Key identifier, @NotNull Category category) {
        this(identifier.asString().replace(':', '_'), category);
    }

    public CategoryAddedPacket {
        if (!IDENTIFIER_PATTERN.matcher(identifier).matches()) throw new IllegalArgumentException("category id does not match pattern " + IDENTIFIER_PATTERN.pattern());
        if (category.name().length() > 16) throw new IllegalArgumentException("category name is too long, found " + category.name().length() + " characters, maximum is 16");
        if (category.description() != null && category.description().length() > 32767) throw new IllegalArgumentException("category description is too long, found " + category.description().length() + " characters, maximum is 32767");
        if (category.icon() != null) {
            if (category.icon().length != 16) throw new IllegalArgumentException("category icon is not 16x16, found " + category.icon().length + "x" + category.icon().length);
            for (int[] row : category.icon()) {
                if (row.length != 16) throw new IllegalArgumentException("category icon is not 16x16, found " + row.length + "x16");
            }
        }
    }

    @Override
    public @NotNull String id() {
        return "voicechat:add_category";
    }

    @Override
    public NetworkBuffer.@NotNull Type<CategoryAddedPacket> serializer() {
        return SERIALIZER;
    }

}
